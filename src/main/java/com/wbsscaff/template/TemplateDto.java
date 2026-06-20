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

        public static NodeResponse from(WbsTemplateNode n) {
            NodeResponse r = new NodeResponse();
            r.id = n.getId(); r.parentId = n.getParentId();
            r.title = n.getTitle(); r.sortOrder = n.getSortOrder();
            return r;
        }
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
}
