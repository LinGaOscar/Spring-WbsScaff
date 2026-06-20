package com.wbsscaff.wbs;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.project.ProjectService;
import com.wbsscaff.template.TemplateDto;
import com.wbsscaff.template.TemplateService;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WbsController {

    private final WbsService wbsService;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final TemplateService templateService;

    @GetMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<List<WbsDto.Response>> getNodes(@PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(wbsService.getNodes(projectId));
    }

    @PostMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<WbsDto.Response> createNode(@PathVariable Long projectId,
            @Valid @RequestBody WbsDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.createNode(projectId, req)));
    }

    @PutMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<WbsDto.Response> updateNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @RequestBody WbsDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.updateNode(nodeId, req)));
    }

    @DeleteMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<Void> deleteNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.deleteNode(nodeId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/projects/{projectId}/nodes/reorder")
    public ApiResponse<Void> reorder(@PathVariable Long projectId,
            @RequestBody List<WbsDto.ReorderItem> items,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.reorder(projectId, items);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/projects/{projectId}/nodes/init")
    public ApiResponse<Void> initFromTemplate(@PathVariable Long projectId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        templateService.applyToProject(body.get("templateId"), projectId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/projects/{projectId}/nodes/import-template")
    public ApiResponse<Void> importTemplate(@PathVariable Long projectId,
            @RequestBody String json,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        checkMember(projectId, userDetails);
        templateService.importJson(json, projectId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/projects/{projectId}/save-as-template")
    public ApiResponse<TemplateDto.TemplateResponse> saveAsTemplate(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.saveProjectAsTemplate(projectId, user.getId(), body.get("name"))));
    }

    // 規格要求的路徑：套用模板至指定專案（檢查成員或 ADMIN 權限）
    @PostMapping("/api/projects/{projectId}/apply-template/{templateId}")
    public ApiResponse<Void> applyTemplate(@PathVariable Long projectId,
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.isMember(projectId, caller.getId()) && caller.getRole() != User.Role.ADMIN) {
            throw new SecurityException("非專案成員無法套用模板");
        }
        templateService.applyToProject(templateId, projectId);
        return ApiResponse.ok(null);
    }

    // ADMIN 角色可繞過成員檢查，直接操作任何專案的 WBS
    private void checkMember(Long projectId, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (user.getRole() == User.Role.ADMIN) return;
        if (!projectService.isMember(projectId, user.getId())) {
            throw new SecurityException("您不是此專案成員");
        }
    }
}
