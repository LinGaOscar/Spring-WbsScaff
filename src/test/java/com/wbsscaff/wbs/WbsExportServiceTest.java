package com.wbsscaff.wbs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WbsExportServiceTest {

    @Autowired WbsExportService exportService;

    @Test
    void buildCsv_emptyNodes_containsBomAndHeader() {
        String csv = exportService.buildCsv(List.of());
        assertThat(csv).startsWith("﻿");
        assertThat(csv).contains("編號");
        assertThat(csv).contains("標題");
    }

    @Test
    void buildXlsx_emptyNodes_returnsNonEmptyBytes() throws Exception {
        byte[] bytes = exportService.buildXlsx(List.of());
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void buildCsv_withNodes_containsHierarchicalNumbering() {
        WbsDto.Response root  = makeNode(1L, null, "大項一", 0);
        WbsDto.Response child = makeNode(2L, 1L,   "子項一", 0);
        String csv = exportService.buildCsv(List.of(root, child));
        assertThat(csv).contains("\"1\"");
        assertThat(csv).contains("\"1.1\"");
        assertThat(csv).contains("大項一");
        assertThat(csv).contains("子項一");
    }

    private WbsDto.Response makeNode(Long id, Long parentId, String title, int sortOrder) {
        WbsDto.Response r = new WbsDto.Response();
        r.setId(id);
        r.setParentId(parentId);
        r.setTitle(title);
        r.setSortOrder(sortOrder);
        r.setStatus(WbsNode.Status.NOT_STARTED);
        return r;
    }
}
