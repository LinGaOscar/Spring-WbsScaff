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
        // 自動繼承建立者所屬科（部門隔離的核心）
        if (creator.getDepartment() != null) {
            p.setDepartment(creator.getDepartment());
        }
        Project saved = projectRepository.save(p);
        addMember(saved.getId(), creatorId, creatorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Project> listForUser(User user) {
        return switch (user.getRole()) {
            case DIRECTOR -> {
                // 部長自建的專案（掛在部層級）+ 下屬科的所有專案
                List<Project> own = projectRepository.findByDepartmentId(user.getDepartment().getId());
                List<Long> sectionIds = departmentRepository.findByParentId(user.getDepartment().getId())
                    .stream().map(Department::getId).toList();
                List<Project> sectionProjects = sectionIds.isEmpty()
                    ? List.of() : projectRepository.findBySectionIds(sectionIds);
                yield java.util.stream.Stream.concat(own.stream(), sectionProjects.stream()).toList();
            }
            case SECTION_CHIEF, PROJECT_LEADER ->
                // 科長/Leader 查本科所有專案
                projectRepository.findByDepartmentId(user.getDepartment().getId());
            case PROJECT_MEMBER ->
                // Member 只能看自己被加入的專案
                projectRepository.findByMemberOrOwner(user.getId());
        };
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

    // 判斷是否有寫入（編輯WBS）權限
    public boolean canWriteProject(Long projectId, User user) {
        Project project = getById(projectId);
        return switch (user.getRole()) {
            case DIRECTOR ->
                // 部長只能寫自己建立的專案（owner），下屬科的專案僅能讀
                project.getOwner() != null && project.getOwner().getId().equals(user.getId());
            case SECTION_CHIEF ->
                // 科長可寫本科所有專案
                project.getDepartment() != null
                && project.getDepartment().getId().equals(user.getDepartment().getId());
            case PROJECT_LEADER, PROJECT_MEMBER ->
                // Leader/Member 只能寫自己被加入的專案
                isMember(projectId, user.getId());
        };
    }

    // 判斷是否有讀取權限
    public boolean canReadProject(Long projectId, User user) {
        Project project = getById(projectId);
        return switch (user.getRole()) {
            case DIRECTOR -> {
                if (project.getDepartment() == null) yield false;
                Department dept = project.getDepartment();
                // 自己建立的部層級專案，或下屬科的專案
                yield dept.getId().equals(user.getDepartment().getId())
                    || (dept.getParent() != null
                        && dept.getParent().getId().equals(user.getDepartment().getId()));
            }
            case SECTION_CHIEF, PROJECT_LEADER ->
                // 科長/Leader 可讀本科所有專案
                project.getDepartment() != null
                && project.getDepartment().getId().equals(user.getDepartment().getId());
            case PROJECT_MEMBER ->
                // Member 只能讀自己被加入的專案
                isMember(projectId, user.getId());
        };
    }
}
