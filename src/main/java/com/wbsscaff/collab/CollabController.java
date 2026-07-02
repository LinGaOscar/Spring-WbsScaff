package com.wbsscaff.collab;

import com.wbsscaff.project.ProjectService;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsDto;
import com.wbsscaff.wbs.WbsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CollabController {

    private final SimpMessagingTemplate broker;
    private final CollabService collabService;
    private final WbsService wbsService;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    // 寫入操作：使用統一的 canWriteProject 判斷（部長、非成員 PROJECT_MEMBER 均被拒絕）
    private void checkWriteAccess(Long projectId, User user) {
        if (!projectService.canWriteProject(projectId, user)) {
            throw new SecurityException("無編輯此專案的權限");
        }
    }

    /** 用戶訂閱 presence 頻道時，登記 JOIN 並廣播給其他人，同時回傳當前在線列表 */
    @SubscribeMapping("/project/{projectId}/presence")
    public List<PresenceMessage> onSubscribePresence(
            @DestinationVariable Long projectId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        collabService.join(projectId, user.getId(), user.getDisplayName());

        PresenceMessage joinMsg = new PresenceMessage();
        joinMsg.setType(PresenceMessage.Type.JOIN);
        joinMsg.setUserId(user.getId());
        joinMsg.setDisplayName(user.getDisplayName());
        joinMsg.setColor(collabService.userColor(user.getId()));
        broker.convertAndSend("/topic/project/" + projectId + "/presence", joinMsg);

        return collabService.getOnlineUsers(projectId);
    }

    /** 接收節點更新請求，持久化後廣播給所有協作者 */
    @MessageMapping("/project/{projectId}/node/update")
    public void onNodeUpdate(@DestinationVariable Long projectId,
            @Payload WbsDto.UpdateRequest req,
            @Header("nodeId") Long nodeId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        checkWriteAccess(projectId, user);
        WbsDto.Response updated = WbsDto.Response.from(wbsService.updateNode(projectId, nodeId, req));

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_UPDATE);
        msg.setNodeId(nodeId);
        msg.setPayload(updated);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId());
        ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    /** 接收節點新增請求，持久化後廣播 */
    @MessageMapping("/project/{projectId}/node/create")
    public void onNodeCreate(@DestinationVariable Long projectId,
            @Payload WbsDto.CreateRequest req, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        checkWriteAccess(projectId, user);
        WbsDto.Response created = WbsDto.Response.from(wbsService.createNode(projectId, req));

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_CREATE);
        msg.setNodeId(created.getId());
        msg.setPayload(created);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId()); ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    /** 接收節點刪除請求，持久化後廣播 */
    @MessageMapping("/project/{projectId}/node/delete")
    public void onNodeDelete(@DestinationVariable Long projectId,
            @Header("nodeId") Long nodeId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        checkWriteAccess(projectId, user);
        wbsService.deleteNode(projectId, nodeId);

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_DELETE);
        msg.setNodeId(nodeId);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId()); ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);

        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    /**
     * 前端 onConnect 後 publish 到 /app/project/{id}/join，讓後端廣播 presence
     * SockJS 無法攔截 onConnect 事件，前端主動 publish 以補足 presence 廣播
     */
    @MessageMapping("/project/{projectId}/join")
    public void handleJoin(@DestinationVariable Long projectId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        collabService.join(projectId, user.getId(), user.getDisplayName());
        String color = collabService.getUserColor(user.getId());
        PresenceMessage msg = new PresenceMessage();
        msg.setType(PresenceMessage.Type.JOIN);
        msg.setUserId(user.getId());
        msg.setDisplayName(user.getDisplayName());
        msg.setColor(color);
        broker.convertAndSend("/topic/project/" + projectId + "/presence", msg);
    }

    /**
     * 前端 beforeunload 時 publish 到 /app/project/{id}/leave 讓後端廣播離線
     * beforeunload 無法等待 HTTP，改用 WebSocket publish 確保離線訊息送出
     */
    @MessageMapping("/project/{projectId}/leave")
    public void handleLeave(@DestinationVariable Long projectId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        collabService.leave(projectId, user.getId());
        String color = collabService.getUserColor(user.getId());
        PresenceMessage msg = new PresenceMessage();
        msg.setType(PresenceMessage.Type.LEAVE);
        msg.setUserId(user.getId());
        msg.setDisplayName(user.getDisplayName());
        msg.setColor(color);
        broker.convertAndSend("/topic/project/" + projectId + "/presence", msg);
    }

    /** 節點拖曳排序：批次更新 parentId + sortOrder，廣播給所有協作者 */
    @MessageMapping("/project/{projectId}/node/reorder")
    public void onReorder(@DestinationVariable Long projectId,
            @Payload List<WbsDto.ReorderWithParentItem> items, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        checkWriteAccess(projectId, user);
        wbsService.reorderWithParent(projectId, items);

        NodeChangeMessage msg = new NodeChangeMessage();
        msg.setType(NodeChangeMessage.Type.NODE_REORDER);
        msg.setPayload(items);
        NodeChangeMessage.UserInfo ui = new NodeChangeMessage.UserInfo();
        ui.setUserId(user.getId()); ui.setDisplayName(user.getDisplayName());
        ui.setColor(collabService.userColor(user.getId()));
        msg.setOperator(ui);
        broker.convertAndSend("/topic/project/" + projectId + "/nodes", msg);
    }

    /** 接收游標位置，以 server 端用戶資訊覆蓋後轉發給同一專案的協作者，防止客戶端偽造身份 */
    @MessageMapping("/project/{projectId}/cursor")
    public void onCursor(@DestinationVariable Long projectId,
            @Payload CursorMessage cursor, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        // 以 server 端的用戶資訊覆蓋，防止客戶端偽造
        cursor.setUserId(user.getId());
        cursor.setDisplayName(user.getDisplayName());
        cursor.setColor(collabService.userColor(user.getId()));
        broker.convertAndSend("/topic/project/" + projectId + "/cursors", cursor);
    }
}
