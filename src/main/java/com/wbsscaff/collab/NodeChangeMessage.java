package com.wbsscaff.collab;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeChangeMessage {
    public enum Type { NODE_UPDATE, NODE_CREATE, NODE_DELETE }

    private Type type;
    private Long nodeId;
    private Object payload;
    private UserInfo operator;
    private LocalDateTime timestamp = LocalDateTime.now();

    @Data
    public static class UserInfo {
        private Long userId;
        private String displayName;
        private String color;
    }
}
