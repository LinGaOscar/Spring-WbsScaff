package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public Project createProject(ProjectDto.CreateRequest req, Long creatorId) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project p = new Project();
        p.setName(req.getName());
        p.setOwner(creator);
        p.setCreatedBy(creator);
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            p.setDepartment(dept);
        } else if (creator.getDepartment() != null) {
            // 自動繼承建立者所屬部門，確保部門隔離生效
            p.setDepartment(creator.getDepartment());
        }
        Project saved = projectRepository.save(p);
        addMember(saved.getId(), creatorId, creatorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Project> listForUser(User user) {
        // ADMIN / IT_USER 可查所有專案（跨部門稽核）
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.IT_USER) {
            return projectRepository.findAll();
        }
        // 一般成員：有部門者僅可見同部門專案；無部門者依成員資格
        if (user.getDepartment() != null) {
            return projectRepository.findByMemberOrOwnerAndDepartment(
                user.getId(), user.getDepartment().getId());
        }
        return projectRepository.findByMemberOrOwner(user.getId());
    }

    @Transactional
    public void addMember(Long projectId, Long userId, Long assignedById) {
        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) return;
        ProjectMember m = new ProjectMember();
        m.setId(new ProjectMemberId(projectId, userId));
        User assignedBy = userRepository.findById(assignedById)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        m.setAssignedBy(assignedBy);
        memberRepository.save(m);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        memberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    @Transactional
    public void changeOwner(Long projectId, Long newOwnerId) {
        Project p = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        User newOwner = userRepository.findById(newOwnerId)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        p.setOwner(newOwner);
        projectRepository.save(p);
        addMember(projectId, newOwnerId, newOwnerId);
    }

    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId);
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
    }
}
