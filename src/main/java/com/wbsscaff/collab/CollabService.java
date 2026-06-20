package com.wbsscaff.collab;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollabService {

    // 依 spec 定義的色板（Finding 5）
    private static final String[] COLORS = {
        "#e74c3c", "#3498db", "#2ecc71", "#f39c12",
        "#9b59b6", "#1abc9c", "#e67e22", "#34495e"
    };

    // projectId → (userId → PresenceMessage)，記憶體內追蹤在線用戶，重啟後清空
    public final Map<Long, Map<Long, PresenceMessage>> sessions = new ConcurrentHashMap<>();

    public String userColor(Long userId) {
        // 使用位元遮罩確保非負數，避免 userId 為負數時造成陣列越界（Finding 1）
        int colorIndex = (int) (userId & 0x7FFFFFFFL) % COLORS.length;
        return COLORS[colorIndex];
    }

    /** getUserColor 為 userColor 的別名，供 CollabController join/leave 端點呼叫 */
    public String getUserColor(Long userId) {
        return userColor(userId);
    }

    public void join(Long projectId, Long userId, String displayName) {
        sessions.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
        PresenceMessage msg = new PresenceMessage();
        msg.setType(PresenceMessage.Type.JOIN);
        msg.setUserId(userId);
        msg.setDisplayName(displayName);
        msg.setColor(userColor(userId));
        sessions.get(projectId).put(userId, msg);
    }

    public void leave(Long projectId, Long userId) {
        if (sessions.containsKey(projectId)) {
            sessions.get(projectId).remove(userId);
        }
    }

    public List<PresenceMessage> getOnlineUsers(Long projectId) {
        return sessions.getOrDefault(projectId, Collections.emptyMap())
            .values().stream().toList();
    }
}
