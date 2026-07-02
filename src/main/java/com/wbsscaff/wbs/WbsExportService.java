package com.wbsscaff.wbs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.stream.*;

@Service
public class WbsExportService {

    private static final String[] HEADERS = {"編號", "層級", "標題", "負責人", "開始日期", "結束日期", "狀態"};
    private static final int[]    COL_WIDTHS = {8, 6, 40, 12, 12, 12, 12};

    public byte[] buildXlsx(List<WbsDto.Response> nodes) throws IOException {
        Map<Long, String>         nums    = buildNumbering(nodes);
        List<WbsDto.Response>     ordered = buildOrdered(nodes);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = wb.createSheet("WBS");
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < COL_WIDTHS.length; i++)
                sheet.setColumnWidth(i, COL_WIDTHS[i] * 256);

            XSSFCellStyle headerStyle = buildHeaderStyle(wb);
            Map<WbsNode.Status, XSSFCellStyle> statusStyles = buildStatusStyles(wb);

            // 建立標題列
            XSSFRow hdr = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                XSSFCell c = hdr.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            // 依序輸出每個節點
            int rowNum = 1;
            for (WbsDto.Response node : ordered) {
                XSSFRow row   = sheet.createRow(rowNum++);
                WbsNode.Status status = node.getStatus() != null ? node.getStatus() : WbsNode.Status.NOT_STARTED;
                XSSFCellStyle style = statusStyles.get(status);
                Object[] vals = {
                    nums.getOrDefault(node.getId(), ""),
                    node.getParentId() == null ? 1 : 2,
                    node.getTitle() != null ? node.getTitle() : "",
                    node.getOwner() != null ? node.getOwner() : "",
                    node.getStartDate() != null ? node.getStartDate().toString() : "",
                    node.getEndDate()   != null ? node.getEndDate().toString()   : "",
                    statusLabel(status)
                };
                for (int i = 0; i < vals.length; i++) {
                    XSSFCell c = row.createCell(i);
                    if (vals[i] instanceof Integer v) c.setCellValue(v);
                    else c.setCellValue((String) vals[i]);
                    c.setCellStyle(style);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public String buildCsv(List<WbsDto.Response> nodes) {
        Map<Long, String>      nums    = buildNumbering(nodes);
        List<WbsDto.Response>  ordered = buildOrdered(nodes);

        // UTF-8 BOM 確保 Excel 正確識別中文
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(Arrays.stream(HEADERS).map(h -> "\"" + h + "\"")
            .collect(Collectors.joining(","))).append("\n");

        for (WbsDto.Response node : ordered) {
            WbsNode.Status status = node.getStatus() != null ? node.getStatus() : WbsNode.Status.NOT_STARTED;
            String[] row = {
                nums.getOrDefault(node.getId(), ""),
                String.valueOf(node.getParentId() == null ? 1 : 2),
                node.getTitle() != null ? node.getTitle() : "",
                node.getOwner() != null ? node.getOwner() : "",
                node.getStartDate() != null ? node.getStartDate().toString() : "",
                node.getEndDate()   != null ? node.getEndDate().toString()   : "",
                statusLabel(status)
            };
            sb.append(Arrays.stream(row).map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(","))).append("\n");
        }
        return sb.toString();
    }

    // 建立根 → 子的排列順序，確保匯出時層次清晰
    private List<WbsDto.Response> buildOrdered(List<WbsDto.Response> nodes) {
        Map<Long, List<WbsDto.Response>> childMap = nodes.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(WbsDto.Response::getParentId,
                Collectors.collectingAndThen(Collectors.toList(),
                    l -> { l.sort(Comparator.comparing(WbsDto.Response::getSortOrder,
                               Comparator.nullsLast(Comparator.naturalOrder()))); return l; })));
        List<WbsDto.Response> result = new ArrayList<>();
        nodes.stream()
            .filter(n -> n.getParentId() == null)
            .sorted(Comparator.comparing(WbsDto.Response::getSortOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(root -> {
                result.add(root);
                result.addAll(childMap.getOrDefault(root.getId(), List.of()));
            });
        return result;
    }

    // 依 DFS 順序為每個節點指派層級編號（如 "1"、"1.1"）
    private Map<Long, String> buildNumbering(List<WbsDto.Response> nodes) {
        Map<Long, String> result = new LinkedHashMap<>();
        Map<Long, List<WbsDto.Response>> childMap = nodes.stream()
            .filter(n -> n.getParentId() != null)
            .collect(Collectors.groupingBy(WbsDto.Response::getParentId,
                Collectors.collectingAndThen(Collectors.toList(),
                    l -> { l.sort(Comparator.comparing(WbsDto.Response::getSortOrder,
                               Comparator.nullsLast(Comparator.naturalOrder()))); return l; })));
        List<WbsDto.Response> roots = nodes.stream()
            .filter(n -> n.getParentId() == null)
            .sorted(Comparator.comparing(WbsDto.Response::getSortOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        for (int i = 0; i < roots.size(); i++) {
            String rootNum = String.valueOf(i + 1);
            result.put(roots.get(i).getId(), rootNum);
            List<WbsDto.Response> children = childMap.getOrDefault(roots.get(i).getId(), List.of());
            for (int j = 0; j < children.size(); j++)
                result.put(children.get(j).getId(), rootNum + "." + (j + 1));
        }
        return result;
    }

    // 標題列使用藍底（#4472C4）白色粗體，符合規格
    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(new byte[]{
            (byte) 0x44, (byte) 0x72, (byte) 0xC4}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private Map<WbsNode.Status, XSSFCellStyle> buildStatusStyles(XSSFWorkbook wb) {
        Map<WbsNode.Status, XSSFCellStyle> m = new EnumMap<>(WbsNode.Status.class);
        m.put(WbsNode.Status.NOT_STARTED, colorStyle(wb, "EEEEEE"));
        m.put(WbsNode.Status.IN_PROGRESS, colorStyle(wb, "DDEEFF"));
        m.put(WbsNode.Status.DONE,        colorStyle(wb, "DDFFDD"));
        return m;
    }

    private XSSFCellStyle colorStyle(XSSFWorkbook wb, String hex) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private String statusLabel(WbsNode.Status s) {
        return switch (s) {
            case NOT_STARTED -> "未開始";
            case IN_PROGRESS -> "進行中";
            case DONE        -> "完成";
        };
    }
}
