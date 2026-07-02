package com.wbsscaff.wbs;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WbsControllerExportTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired WbsRepository wbsRepository;

    private Long projectId;

    @BeforeEach
    void setUp() {
        // 建立科別，讓 SECTION_CHIEF 的 canReadProject 能通過部門比對
        Department deptRaw = new Department();
        deptRaw.setName("匯出測試科");
        final Department dept = departmentRepository.save(deptRaw);

        User user = userRepository.findByEmail("export-test@test.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("export-test@test.com");
            u.setPasswordHash("x");
            u.setDisplayName("Export Tester");
            u.setRole(User.Role.SECTION_CHIEF);
            u.setDepartment(dept);
            return userRepository.save(u);
        });

        // 建立測試專案，部門設定與使用者相同，讓科長能讀取
        Project project = new Project();
        project.setName("匯出測試專案");
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
    @WithMockUser(username = "export-test@test.com", roles = "SECTION_CHIEF")
    void exportXlsx_returns200WithSpreadsheetContentType() throws Exception {
        mockMvc.perform(get("/api/projects/{id}/nodes/export.xlsx", projectId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("spreadsheetml")));
    }

    @Test
    @WithMockUser(username = "export-test@test.com", roles = "SECTION_CHIEF")
    void exportCsv_returns200WithCsvContentType() throws Exception {
        mockMvc.perform(get("/api/projects/{id}/nodes/export.csv", projectId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/csv")));
    }

    @Test
    void exportXlsx_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/projects/{id}/nodes/export.xlsx", projectId))
            .andExpect(status().is3xxRedirection());
    }
}
