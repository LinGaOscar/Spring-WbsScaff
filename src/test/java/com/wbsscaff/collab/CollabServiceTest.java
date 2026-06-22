package com.wbsscaff.collab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CollabServiceTest {

    @Autowired CollabService collabService;

    @Test
    void userColor_deterministicByUserId() {
        String c1 = collabService.userColor(1L);
        String c2 = collabService.userColor(1L);
        assertThat(c1).isEqualTo(c2);
        assertThat(c1).startsWith("#");
    }

    @Test
    void join_then_leave_updatesOnlineList() {
        Long projectId = 999L;
        collabService.join(projectId, 1L, "Alice");
        collabService.join(projectId, 2L, "Bob");
        assertThat(collabService.getOnlineUsers(projectId)).hasSize(2);

        collabService.leave(projectId, 1L);
        assertThat(collabService.getOnlineUsers(projectId)).hasSize(1);
    }
}
