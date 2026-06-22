package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProjectServiceTest {

    @Autowired ProjectService projectService;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User manager, member;
    Department dept;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        deptRepository.deleteAll();

        dept = new Department(); dept.setName("IT"); deptRepository.save(dept);

        manager = new User(); manager.setEmail("mgr@t.com");
        manager.setPasswordHash(passwordEncoder.encode("p")); manager.setDisplayName("Manager");
        manager.setRole(User.Role.SECTION_CHIEF); manager.setDepartment(dept);
        userRepository.save(manager);

        member = new User(); member.setEmail("mem@t.com");
        member.setPasswordHash(passwordEncoder.encode("p")); member.setDisplayName("Member");
        member.setRole(User.Role.PROJECT_MEMBER); userRepository.save(member);
    }

    @Test
    void createProject_setsOwnerAsCreator() {
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setName("新專案"); req.setDepartmentId(dept.getId());
        Project proj = projectService.createProject(req, manager.getId());
        assertThat(proj.getOwner().getId()).isEqualTo(manager.getId());
    }

    @Test
    void addMember_memberCanBeFound() {
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setName("P"); req.setDepartmentId(dept.getId());
        Project proj = projectService.createProject(req, manager.getId());

        projectService.addMember(proj.getId(), member.getId(), manager.getId());

        assertThat(projectService.isMember(proj.getId(), member.getId())).isTrue();
    }
}
