package com.wbsscaff.project;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    @GetMapping("/projects")
    public String projectsPage() { return "project/list"; }

    @GetMapping("/projects/{id}")
    public String projectDetailPage(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.canReadProject(id, user)) {
            return "redirect:/projects";
        }
        Project project = projectService.getById(id);
        // 無寫入權限（含部長看下屬科專案）則唯讀
        boolean readOnly = !projectService.canWriteProject(id, user);
        model.addAttribute("project", project);
        model.addAttribute("readOnly", readOnly);
        return "project/detail";
    }

    @GetMapping("/api/projects")
    @ResponseBody
    public ApiResponse<List<ProjectDto.Response>> listProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(
            projectService.listForUser(user)
                .stream().map(ProjectDto.Response::from).toList());
    }

    @PostMapping("/api/projects")
    @ResponseBody
    public ApiResponse<ProjectDto.Response> createProject(
            @Valid @RequestBody ProjectDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        // 只有科長與專案Leader才能建立專案
        if (!user.canCreateProject()) {
            throw new SecurityException("您沒有建立專案的權限");
        }
        return ApiResponse.ok(ProjectDto.Response.from(
            projectService.createProject(req, user.getId())));
    }

    @GetMapping("/api/projects/{id}")
    @ResponseBody
    public ApiResponse<ProjectDto.Response> getProject(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkProjectReadAccess(id, userDetails);
        return ApiResponse.ok(ProjectDto.Response.from(projectService.getById(id)));
    }

    @GetMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<List<ProjectDto.MemberResponse>> getMembers(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkProjectReadAccess(id, userDetails);
        return ApiResponse.ok(memberRepository.findByIdProjectId(id)
            .stream().map(ProjectDto.MemberResponse::from).toList());
    }

    private void checkProjectReadAccess(Long id, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.canReadProject(id, user)) {
            throw new SecurityException("無存取權限");
        }
    }

    @PostMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project project = projectService.getById(id);
        // 科長可管理本科任何專案成員；專案負責人可管理自己的專案
        boolean isSectionChief = caller.getRole() == User.Role.SECTION_CHIEF
            && project.getDepartment() != null
            && project.getDepartment().getId().equals(caller.getDepartment().getId());
        boolean isProjectOwner = project.getOwner().getId().equals(caller.getId());
        if (!isSectionChief && !isProjectOwner) {
            throw new SecurityException("只有科長或專案負責人才可管理成員");
        }
        projectService.addMember(id, body.get("userId"), caller.getId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/projects/{id}/members/{userId}")
    @ResponseBody
    public ApiResponse<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project project = projectService.getById(id);
        boolean isSectionChief = caller.getRole() == User.Role.SECTION_CHIEF
            && project.getDepartment() != null
            && project.getDepartment().getId().equals(caller.getDepartment().getId());
        boolean isProjectOwner = project.getOwner().getId().equals(caller.getId());
        if (!isSectionChief && !isProjectOwner) {
            throw new SecurityException("只有科長或專案負責人才可移除成員");
        }
        projectService.removeMember(id, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/projects/{id}/owner")
    @ResponseBody
    public ApiResponse<Void> changeOwner(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 科長（本科專案）或專案 owner（含部長自建）可變更負責人
        User caller = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        Project project = projectService.getById(id);
        boolean isSectionChief = caller.getRole() == User.Role.SECTION_CHIEF
            && project.getDepartment() != null
            && project.getDepartment().getId().equals(caller.getDepartment().getId());
        boolean isProjectOwner = project.getOwner().getId().equals(caller.getId());
        if (!isSectionChief && !isProjectOwner) throw new SecurityException("無權變更負責人");
        projectService.changeOwner(id, body.get("userId"));
        return ApiResponse.ok(null);
    }
}
