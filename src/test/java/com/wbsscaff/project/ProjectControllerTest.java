package com.wbsscaff.project;

import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        // 建立與 @WithMockUser username 對應的資料庫使用者
        if (userRepository.findByEmail("admin@test.com").isEmpty()) {
            User u = new User();
            u.setEmail("admin@test.com");
            u.setPasswordHash("x");
            u.setDisplayName("Admin User");
            u.setRole(User.Role.SECTION_CHIEF);
            userRepository.save(u);
        }
    }

    @AfterEach
    void tearDown() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "SECTION_CHIEF")
    void listProjects_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listProjects_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().is3xxRedirection());
    }
}
