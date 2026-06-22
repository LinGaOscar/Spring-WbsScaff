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

    // 寫入操作：IT_USER 唯讀，非成員禁止
    private void checkWriteAccess(Long projectId, User user) {
        if (user.getRole() == User.Role.IT_USER) {
            throw new SecurityException("IT_User 僅有唯讀權限");
        }
        if (user.getRole() != User.Role.ADMIN
                && !projectService.isMember(projectId, user.getId())) {
            throw new SecurityException("您不是此專案成員");
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
     * 前端主動發送 JOIN（Finding 4）
     * 前端 onConnect 後 publish 到 /app/project/{id}/join，讓後端廣播 presence
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
     * 前端主動發送 LEAVE（Finding 4）
     * beforeunload 時 publish 到 /app/project/{id}/leave 讓後端廣播離線
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

    /** 接收游標位置，轉發給同一專案的協作者 */
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
