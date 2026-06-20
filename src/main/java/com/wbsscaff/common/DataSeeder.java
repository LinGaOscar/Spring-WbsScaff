package com.wbsscaff.common;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDepartments();
        seedAdminUser();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) return;
        for (String name : new String[]{ "資訊部", "業務部", "財務部", "人資部", "產品部" }) {
            Department dept = new Department();
            dept.setName(name);
            departmentRepository.save(dept);
        }
        log.info("已植入 5 個預設部門");
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@wbsscaff.com").isPresent()) return;
        User admin = new User();
        admin.setEmail("admin@wbsscaff.com");
        admin.setPasswordHash(passwordEncoder.encode("admin1234"));
        admin.setDisplayName("系統管理員");
        admin.setRole(User.Role.ADMIN);
        admin.setCanCreateProject(true);
        userRepository.save(admin);
        log.info("已建立初始 ADMIN：admin@wbsscaff.com / admin1234");
    }
}
