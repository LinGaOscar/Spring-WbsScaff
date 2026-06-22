package com.wbsscaff.wbs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class WbsQuickItemDto {

    @Data
    public static class Response {
        private Long id;
        private String title;
        private String category;
        private int sortOrder;
        private String requirementDoc;

        public static Response from(WbsQuickItem item) {
            Response r = new Response();
            r.id = item.getId();
            r.title = item.getTitle();
            r.category = item.getCategory();
            r.sortOrder = item.getSortOrder();
            r.requirementDoc = item.getRequirementDoc();
            return r;
        }
    }

    @Data
    public static class CreateRequest {
        @NotBlank
        private String title;
        private String category;
        private int sortOrder;
        private String requirementDoc;
    }
}
