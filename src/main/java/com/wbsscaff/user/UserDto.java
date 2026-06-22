package com.wbsscaff.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class UserDto {

    @Data
    public static class CreateRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
        @NotBlank private String displayName;
        private Long departmentId;
        @NotNull private User.Role role;
    }

    @Data
    public static class UpdateRequest {
        private String displayName;
        private Long departmentId;
        private User.Role role;
    }

    @Data
    public static class Response {
        private Long id;
        private String email;
        private String displayName;
        private String departmentName;
        private User.Role role;
        private boolean canCreateProject;
        private boolean enabled;

        public static Response from(User user) {
            Response r = new Response();
            r.id = user.getId();
            r.email = user.getEmail();
            r.displayName = user.getDisplayName();
            r.departmentName = user.getDepartment() != null ? user.getDepartment().getName() : null;
            r.role = user.getRole();
            r.canCreateProject = user.isCanCreateProject();
            r.enabled = user.isEnabled();
            return r;
        }
    }
}
