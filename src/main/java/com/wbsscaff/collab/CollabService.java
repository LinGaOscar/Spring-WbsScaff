package com.wbsscaff.collab;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollabService {

    private static final String[] COLORS = {
        "#4A90D9","#E74C3C","#27AE60","#F39C12",
        "#9B59B6","#1ABC9C","#E67E22","#2980B9"
    };

    // projectId → (userId → PresenceMessage)，記憶體內追蹤在線用戶，重啟後清空
    public final Map<Long, Map<Long, PresenceMessage>> sessions = new ConcurrentHashMap<>();

    public String userColor(Long userId) {
        return COLORS[(int)(userId % COLORS.length)];
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
