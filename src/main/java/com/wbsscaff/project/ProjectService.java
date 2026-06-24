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

    // 建立者自動被加為成員，省去建立後還要手動加入自己的步驟
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

    // 各角色可見範圍：部長看部+全部子科；科長/Leader 看本科；Member 只看被加入的
    @Transactional(readOnly = true)
    public List<Project> listForUser(User user, boolean archived) {
        return switch (user.getRole()) {
            case DIRECTOR -> {
                // 部長可查看直屬部的專案，以及所有子科的專案
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
                // Member 只能看被明確加入的專案，無法瀏覽本科其他專案
                projectRepository.findByMemberOrOwner(user.getId(), archived);
        };
    }

    // 歸檔後專案進入歷史唯讀狀態，資料不刪除
    @Transactional
    public void archiveProject(Long projectId, User caller) {
        Project p = getById(projectId);
        checkArchivePermission(p, caller);
        p.setArchived(true);
        projectRepository.save(p);
    }

    // 還原歸檔讓專案回到可編輯狀態
    @Transactional
    public void unarchiveProject(Long projectId, User caller) {
        Project p = getById(projectId);
        checkArchivePermission(p, caller);
        p.setArchived(false);
        projectRepository.save(p);
    }

    // 歸檔/還原只允許專案負責人或同科科長，防止跨科或一般成員誤操作
    private void checkArchivePermission(Project p, User caller) {
        boolean isOwner = p.getOwner() != null && p.getOwner().getId().equals(caller.getId());
        boolean isChief = caller.getRole() == User.Role.SECTION_CHIEF
            && p.getDepartment() != null
            && p.getDepartment().getId().equals(caller.getDepartment().getId());
        if (!isOwner && !isChief) {
            throw new SecurityException("只有專案負責人或科長可歸檔/還原專案");
        }
    }

    // 冪等操作：已存在的成員不重複新增，避免 unique constraint 衝突
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

    // 踢出成員後立即失去 PROJECT_MEMBER 的 WBS 編輯權限
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        memberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    // 變更負責人時，若新 owner 尚未加入成員清單，自動補加，確保其有編輯權限
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

    // PROJECT_MEMBER 角色的寫入判斷依據
    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId);
    }

    // 統一拋 EntityNotFoundException，避免各處 findById 遺漏 404 處理
    public Project getById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
    }

    // 系統最核心的安全閘：DIRECTOR 只能讀，不能改；歸檔專案對全員強制唯讀
    public boolean canWriteProject(Long projectId, User user) {
        Project project = getById(projectId);
        // 歸檔專案全員唯讀
        if (project.isArchived()) return false;
        return switch (user.getRole()) {
            case DIRECTOR ->
                // 部長即使建立了專案也只能唯讀，不影響下屬科的編輯流程
                project.getOwner() != null && project.getOwner().getId().equals(user.getId());
            case SECTION_CHIEF ->
                project.getDepartment() != null
                && project.getDepartment().getId().equals(user.getDepartment().getId());
            case PROJECT_LEADER, PROJECT_MEMBER ->
                isMember(projectId, user.getId());
        };
    }

    // 部長透過 parent 關聯查子科（部門樹兩層：部 → 科），科長/Leader 只看本科，Member 僅看被加入的
    public boolean canReadProject(Long projectId, User user) {
        Project project = getById(projectId);
        return switch (user.getRole()) {
            case DIRECTOR -> {
                if (project.getDepartment() == null) yield false;
                Department dept = project.getDepartment();
                // 直接屬於本部，或屬於本部的子科
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
