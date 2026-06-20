package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserDto.CreateRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("電子郵件已存在：" + req.getEmail());
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setRole(req.getRole());
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }

    public List<User> listUsers() {
        return userRepository.findByEnabledTrue();
    }

    @Transactional
    public User updateUser(Long id, UserDto.UpdateRequest req) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName());
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void setCanCreateProject(Long id, boolean value) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        user.setCanCreateProject(value);
        userRepository.save(user);
    }
}
