package com.wbsscaff.project;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public String projectDetailPage(@PathVariable Long id, Model model) {
        // 將專案 ID 注入 model，讓 Thymeleaf 安全地輸出，避免 URI 字串解析風險
        model.addAttribute("project", projectService.getById(id));
        return "project/detail";
    }

    @GetMapping("/api/projects")
    @ResponseBody
    public ApiResponse<List<ProjectDto.Response>> listProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        // 使用者不存在視為認證異常，拋出例外由 GlobalExceptionHandler 處理
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(
            projectService.listForUser(user.getId())
                .stream().map(ProjectDto.Response::from).toList());
    }

    @PostMapping("/api/projects")
    @ResponseBody
    public ApiResponse<ProjectDto.Response> createProject(
            @Valid @RequestBody ProjectDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        // 只有可建立專案的用戶或管理員才能新增專案
        if (!user.isCanCreateProject() && user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("您沒有建立專案的權限");
        }
        return ApiResponse.ok(ProjectDto.Response.from(
            projectService.createProject(req, user.getId())));
    }

    @GetMapping("/api/projects/{id}")
    @ResponseBody
    public ApiResponse<ProjectDto.Response> getProject(@PathVariable Long id) {
        // 取得單一專案資訊，供 WBS 編輯器頁面顯示專案名稱
        return ApiResponse.ok(ProjectDto.Response.from(projectService.getById(id)));
    }

    @GetMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<List<ProjectDto.MemberResponse>> getMembers(@PathVariable Long id) {
        return ApiResponse.ok(memberRepository.findByIdProjectId(id)
            .stream().map(ProjectDto.MemberResponse::from).toList());
    }

    @PostMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        // 只有管理員或專案負責人才可新增成員，防止任意成員擴權
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project project = projectService.getById(id);
        if (caller.getRole() != User.Role.ADMIN && !project.getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("只有管理員或專案負責人可以新增成員");
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
        // 只有管理員或專案負責人才可移除成員
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project project = projectService.getById(id);
        if (caller.getRole() != User.Role.ADMIN && !project.getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("只有管理員或專案負責人可以移除成員");
        }
        projectService.removeMember(id, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/projects/{id}/owner")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> changeOwner(
            @PathVariable Long id, @RequestBody Map<String, Long> body) {
        projectService.changeOwner(id, body.get("userId"));
        return ApiResponse.ok(null);
    }
}
