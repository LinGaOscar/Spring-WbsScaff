package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
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
class ProjectRepositoryTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;

    @Test
    void findByDepartmentId_returnsProjects() {
        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User owner = new User(); owner.setEmail("o@test.com"); owner.setPasswordHash("x");
        owner.setDisplayName("Owner"); owner.setRole(User.Role.PROJECT_MEMBER); userRepository.save(owner);

        Project p = new Project(); p.setName("測試專案");
        p.setDepartment(dept); p.setOwner(owner); p.setCreatedBy(owner);
        projectRepository.save(p);

        List<Project> result = projectRepository.findByDepartmentId(dept.getId());
        assertThat(result).hasSize(1);
    }
}
