package com.wbsscaff.template;

import lombok.Data;
import java.util.List;

public class TemplateDto {

    @Data
    public static class NodeResponse {
        private Long id;
        private Long parentId;
        private String title;
        private Integer sortOrder;
        private String notes;

        public static NodeResponse from(WbsTemplateNode n) {
            NodeResponse r = new NodeResponse();
            r.id = n.getId(); r.parentId = n.getParentId();
            r.title = n.getTitle(); r.sortOrder = n.getSortOrder();
            r.notes = n.getNotes();
            return r;
        }
    }

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
    }

    @Data
    public static class NodeCreateRequest {
        private String title;
        private Long parentId;
        private Integer sortOrder;
        private String notes;
    }

    @Data
    public static class NodeUpdateRequest {
        private String title;
        private String notes;
    }

    @Data
    public static class ReorderItem {
        private Long id;
        private Integer sortOrder;
    }

    @Data
    public static class TemplateResponse {
        private Long id;
        private String name;
        private String description;
        private boolean isSystem;
        private boolean isDefault;

        public static TemplateResponse from(WbsTemplate t) {
            TemplateResponse r = new TemplateResponse();
            r.id = t.getId(); r.name = t.getName();
            r.description = t.getDescription();
            r.isSystem = t.isSystem(); r.isDefault = t.isDefault();
            return r;
        }
    }

    @Data
    public static class ListResponse {
        private List<TemplateResponse> system;
        private List<TemplateResponse> custom;
    }

    @Data
    public static class ExportNode {
        private String title;
        private List<ExportNode> children;
    }

    // 與 TemplateResponse 相同結構，提供語意更明確的名稱給 updateTemplate/saveFromProject 使用
    @Data
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private boolean isSystem;
        private boolean isDefault;
        private Long clonedFrom;

        public static Response from(WbsTemplate t) {
            Response r = new Response();
            r.id = t.getId(); r.name = t.getName();
            r.description = t.getDescription();
            r.isSystem = t.isSystem(); r.isDefault = t.isDefault();
            r.clonedFrom = t.getClonedFrom();
            return r;
        }
    }

    // 模板更新請求，欄位均為選填（null 代表不修改）
    @Data
    public static class UpdateRequest {
        private String name;
        private String description;
    }

    // 包含節點清單的完整模板回應，用於 GET /api/templates/{id}
    @Data
    public static class DetailResponse {
        private Long id;
        private String name;
        private String description;
        private boolean isSystem;
        private boolean isDefault;
        private Long clonedFrom;
        private List<NodeResponse> nodes;

        public static DetailResponse from(WbsTemplate t, List<WbsTemplateNode> nodeList) {
            DetailResponse r = new DetailResponse();
            r.id = t.getId(); r.name = t.getName();
            r.description = t.getDescription();
            r.isSystem = t.isSystem(); r.isDefault = t.isDefault();
            r.clonedFrom = t.getClonedFrom();
            r.nodes = nodeList.stream().map(NodeResponse::from).toList();
            return r;
        }
    }
}
