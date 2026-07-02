package com.wbsscaff.wbs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectMemberRepository;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WbsControllerReplaceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired WbsRepository wbsRepository;

    private Long projectId;

    @BeforeEach
    void setUp() {
        // 建立科別與使用者，讓 canWriteProject 權限檢查能通過
        Department deptRaw = new Department();
        deptRaw.setName("Replace 測試科");
        final Department dept = departmentRepository.save(deptRaw);

        User user = userRepository.findByEmail("replace-test@test.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("replace-test@test.com");
            u.setPasswordHash("x");
            u.setDisplayName("Replace Tester");
            u.setRole(User.Role.SECTION_CHIEF);
            u.setDepartment(dept);
            return userRepository.save(u);
        });

        Project project = new Project();
        project.setName("Replace 測試專案");
        project.setOwner(user);
        project.setDepartment(dept);
        project.setArchived(false);
        project = projectRepository.save(project);
        projectId = project.getId();
    }

    @AfterEach
    void tearDown() {
        wbsRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "replace-test@test.com", roles = "SECTION_CHIEF")
    void replaceAll_withValidBody_returns200() throws Exception {
        // 傳入一筆根節點，驗證 HTTP 200 與 JSON 回應包含 title
        List<Map<String, Object>> body = List.of(
            Map.of("title", "根節點A", "status", "NOT_STARTED")
        );
        mockMvc.perform(post("/api/projects/{id}/nodes/replace", projectId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("根節點A"));
    }

    @Test
    @WithMockUser(username = "replace-test@test.com", roles = "SECTION_CHIEF")
    void replaceAll_emptyList_returns200WithEmptyData() throws Exception {
        // 傳入空陣列，舊節點應全部清除
        mockMvc.perform(post("/api/projects/{id}/nodes/replace", projectId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void replaceAll_unauthenticated_redirects() throws Exception {
        // 未登入應被重導向至登入頁（Spring Security 預設行為）
        mockMvc.perform(post("/api/projects/{id}/nodes/replace", projectId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().is3xxRedirection());
    }
}
