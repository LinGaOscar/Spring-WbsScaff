package com.wbsscaff.project;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
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
    public String projectDetailPage() { return "project/detail"; }

    @GetMapping("/api/projects")
    @ResponseBody
    public ApiResponse<List<ProjectDto.Response>> listProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        // 找不到使用者（如測試虛擬帳號）時回傳空清單，避免 500
        return userRepository.findByEmail(userDetails.getUsername())
            .map(user -> ApiResponse.<List<ProjectDto.Response>>ok(
                projectService.listForUser(user.getId())
                    .stream().map(ProjectDto.Response::from).toList()))
            .orElse(ApiResponse.ok(List.of()));
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
        User requester = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        projectService.addMember(id, body.get("userId"), requester.getId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/projects/{id}/members/{userId}")
    @ResponseBody
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
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
