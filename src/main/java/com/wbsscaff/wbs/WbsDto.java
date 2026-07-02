package com.wbsscaff.wbs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

public class WbsDto {

    @Data
    public static class CreateRequest {
        @NotBlank private String title;
        private Long parentId;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private String owner;
        private LocalDate startDate;
        private LocalDate endDate;
        private WbsNode.Status status;
        private String notes;
    }

    @Data
    public static class ReorderItem {
        private Long nodeId;
        private Integer sortOrder;
    }

    @Data
    public static class ReorderWithParentItem {
        private Long nodeId;
        private Long parentId;   // null = L1 根節點
        private Integer sortOrder;
    }

    // JSON 匯入時的樹狀節點格式，對應前端匯出的 JSON 結構
    @Data
    public static class ImportNode {
        @NotBlank private String title;
        private String owner;
        private LocalDate startDate;
        private LocalDate endDate;
        private WbsNode.Status status;
        private String notes;
        private List<ImportNode> children;
    }

    @Data
    public static class Response {
        private Long id;
        private Long parentId;
        private String title;
        private String owner;
        private LocalDate startDate;
        private LocalDate endDate;
        private WbsNode.Status status;
        private String notes;
        private Integer sortOrder;

        public static Response from(WbsNode n) {
            Response r = new Response();
            r.id = n.getId(); r.parentId = n.getParentId();
            r.title = n.getTitle(); r.owner = n.getOwner();
            r.startDate = n.getStartDate(); r.endDate = n.getEndDate();
            r.status = n.getStatus(); r.notes = n.getNotes();
            r.sortOrder = n.getSortOrder();
            return r;
        }
    }
}
