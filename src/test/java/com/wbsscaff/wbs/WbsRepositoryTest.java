package com.wbsscaff.wbs;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class WbsRepositoryTest {

    @Autowired WbsRepository wbsRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;

    @Test
    void findByProjectId_returnsNodes() {
        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User user = new User(); user.setEmail("u@t.com"); user.setPasswordHash("x");
        user.setDisplayName("U"); user.setRole(User.Role.PROJECT_MEMBER); userRepository.save(user);
        Project proj = new Project(); proj.setName("P"); proj.setOwner(user);
        proj.setCreatedBy(user); proj.setDepartment(dept); projectRepository.save(proj);

        WbsNode node = new WbsNode();
        node.setProject(proj); node.setTitle("需求分析"); node.setSortOrder(0);
        wbsRepository.save(node);

        List<WbsNode> nodes = wbsRepository.findByProjectIdOrderBySortOrder(proj.getId());
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getTitle()).isEqualTo("需求分析");
    }
}
