package com.wbsscaff.collab;

import lombok.Data;
import java.time.Instant;

@Data
public class NodeChangeMessage {
    public enum Type { NODE_UPDATE, NODE_CREATE, NODE_DELETE }

    private Type type;
    private Long nodeId;
    private Object payload;
    private UserInfo operator;
    // 使用 Instant 確保 UTC 時區一致性，避免各節點時間不同步（Finding 2）
    private Instant timestamp = Instant.now();

    @Data
    public static class UserInfo {
        private Long userId;
        private String displayName;
        private String color;
    }
}
