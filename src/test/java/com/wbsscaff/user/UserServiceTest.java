package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        deptRepository.deleteAll();
        Department dept = new Department();
        dept.setName("資訊部");
        deptRepository.save(dept);
    }

    @Test
    void createUser_savesWithHashedPassword() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("user@test.com");
        req.setPassword("secret123");
        req.setDisplayName("測試使用者");
        req.setRole(User.Role.MEMBER);

        User created = userService.createUser(req);

        assertThat(created.getId()).isNotNull();
        assertThat(passwordEncoder.matches("secret123", created.getPasswordHash())).isTrue();
    }

    @Test
    void setCanCreateProject_updatesFlag() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("pm@test.com");
        req.setPassword("pass");
        req.setDisplayName("PM");
        req.setRole(User.Role.MEMBER);
        User user = userService.createUser(req);

        userService.setCanCreateProject(user.getId(), true);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isCanCreateProject()).isTrue();
    }

    @Test
    void disableUser_setsEnabledFalse() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("old@test.com");
        req.setPassword("pass");
        req.setDisplayName("舊員工");
        req.setRole(User.Role.MEMBER);
        User user = userService.createUser(req);

        userService.disableUser(user.getId());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isEnabled()).isFalse();
    }
}
