package com.wbsscaff.collab;

import com.wbsscaff.project.ProjectService;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsDto;
import com.wbsscaff.wbs.WbsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 驗證 CollabController.onReorder：
 * - 有寫入權限的用戶可觸發排序廣播，廣播類型為 NODE_REORDER
 * - 無寫入權限的用戶（唯讀/歸檔）拋出 SecurityException，不觸發廣播
 */
@ExtendWith(MockitoExtension.class)
class CollabControllerReorderTest {

    @Mock SimpMessagingTemplate broker;
    @Mock CollabService collabService;
    @Mock WbsService wbsService;
    @Mock UserRepository userRepository;
    @Mock ProjectService projectService;

    @InjectMocks CollabController controller;

    private User authorizedUser;
    private Principal principal;

    @BeforeEach
    void setUp() {
        authorizedUser = new User();
        authorizedUser.setId(1L);
        authorizedUser.setEmail("leader@infotech.com");
        authorizedUser.setDisplayName("Leader");

        principal = () -> "leader@infotech.com";

        when(userRepository.findByEmail("leader@infotech.com"))
            .thenReturn(Optional.of(authorizedUser));
    }

    @Test
    void onReorder_authorizedUser_broadcastsNodeReorder() {
        // 有寫入權限
        when(projectService.canWriteProject(10L, authorizedUser)).thenReturn(true);
        when(collabService.userColor(1L)).thenReturn("#aabbcc");

        List<WbsDto.ReorderWithParentItem> items = List.of(buildItem(100L, null, 0));

        controller.onReorder(10L, items, principal);

        // 驗證 reorderWithParent 被呼叫
        verify(wbsService).reorderWithParent(10L, items);

        // 驗證廣播到正確 topic，類型為 NODE_REORDER
        ArgumentCaptor<NodeChangeMessage> msgCaptor = ArgumentCaptor.forClass(NodeChangeMessage.class);
        verify(broker).convertAndSend(eq("/topic/project/10/nodes"), msgCaptor.capture());
        NodeChangeMessage sent = msgCaptor.getValue();
        assertThat(sent.getType()).isEqualTo(NodeChangeMessage.Type.NODE_REORDER);
        assertThat(sent.getPayload()).isEqualTo(items);
        assertThat(sent.getOperator().getUserId()).isEqualTo(1L);
    }

    @Test
    void onReorder_unauthorizedUser_throwsSecurityException() {
        // 無寫入權限（唯讀成員或歸檔專案）
        when(projectService.canWriteProject(10L, authorizedUser)).thenReturn(false);

        List<WbsDto.ReorderWithParentItem> items = List.of(buildItem(100L, null, 0));

        assertThatThrownBy(() -> controller.onReorder(10L, items, principal))
            .isInstanceOf(SecurityException.class);

        // 未持久化、未廣播
        verifyNoInteractions(wbsService);
        verifyNoInteractions(broker);
    }

    private WbsDto.ReorderWithParentItem buildItem(Long nodeId, Long parentId, int sortOrder) {
        WbsDto.ReorderWithParentItem item = new WbsDto.ReorderWithParentItem();
        item.setNodeId(nodeId);
        item.setParentId(parentId);
        item.setSortOrder(sortOrder);
        return item;
    }
}
