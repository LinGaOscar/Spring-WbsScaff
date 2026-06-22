package com.wbsscaff.collab;

import lombok.Data;

@Data
public class CursorMessage {
    private Long userId;
    private String displayName;
    private String color;
    private Long hoveringNodeId;
    private Long editingNodeId;
}
