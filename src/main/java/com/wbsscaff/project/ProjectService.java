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
        if (creator.getDepartment() != null) {
            p.setDepartment(creator.getDepartment());
        }
        Project saved = projectRepository.save(p);
        addMember(saved.getId(), creatorId, creatorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Project> listForUser(User user, boolean archived) {
        return switch (user.getRole()) {
            case DIRECTOR -> {
                List<Project> own = projectRepository.findByDepartmentIdAndArchived(
                    user.getDepartment().getId(), archived);
                List<Long> sectionIds = departmentRepository.findByParentId(user.getDepartment().getId())
                    .stream().map(Department::getId).toList();
                List<Project> sectionProjects = sectionIds.isEmpty()
                    ? List.of() : projectRepository.findBySectionIds(sectionIds, archived);
                yield java.util.stream.Stream.concat(own.stream(), sectionProjects.stream()).toList();
            }
            case SECTION_CHIEF, PROJECT_LEADER ->
                projectRepository.findByDepartmentIdAndArchived(user.getDepartment().getId(), archived);
            case PROJECT_MEMBER ->
                projectRepository.findByMemberOrOwner(user.getId(), archived);
        };
    }

    @Transactional
    public void archiveProject(Long projectId, User caller) {
        Project p = getById(projectId);
        checkArchivePermission(p, caller);
        p.setArchived(true);
        projectRepository.save(p);
    }

    @Transactional
    public void unarchiveProject(Long projectId, User caller) {
        Project p = getById(projectId);
        checkArchivePermission(p, caller);
        p.setArchived(false);
        projectRepository.save(p);
    }

    private void checkArchivePermission(Project p, User caller) {
        boolean isOwner = p.getOwner() != null && p.getOwner().getId().equals(caller.getId());
        boolean isChief = caller.getRole() == User.Role.SECTION_CHIEF
            && p.getDepartment() != null
            && p.getDepartment().getId().equals(caller.getDepartment().getId());
        if (!isOwner && !isChief) {
            throw new SecurityException("只有專案負責人或科長可歸檔/還原專案");
        }
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

    public boolean canWriteProject(Long projectId, User user) {
        Project project = getById(projectId);
        // 歸檔專案全員唯讀
        if (project.isArchived()) return false;
        return switch (user.getRole()) {
            case DIRECTOR ->
                project.getOwner() != null && project.getOwner().getId().equals(user.getId());
            case SECTION_CHIEF ->
                project.getDepartment() != null
                && project.getDepartment().getId().equals(user.getDepartment().getId());
            case PROJECT_LEADER, PROJECT_MEMBER ->
                isMember(projectId, user.getId());
        };
    }

    public boolean canReadProject(Long projectId, User user) {
        Project project = getById(projectId);
        return switch (user.getRole()) {
            case DIRECTOR -> {
                if (project.getDepartment() == null) yield false;
                Department dept = project.getDepartment();
                yield dept.getId().equals(user.getDepartment().getId())
                    || (dept.getParent() != null
                        && dept.getParent().getId().equals(user.getDepartment().getId()));
            }
            case SECTION_CHIEF, PROJECT_LEADER ->
                project.getDepartment() != null
                && project.getDepartment().getId().equals(user.getDepartment().getId());
            case PROJECT_MEMBER ->
                isMember(projectId, user.getId());
        };
    }
}
