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

    // 頁面載入時一次取得所有節點，前端自行建樹（flat list → tree）
    @GetMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<List<WbsDto.Response>> getNodes(@PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkReadAccess(projectId, userDetails);
        return ApiResponse.ok(wbsService.getNodes(projectId));
    }

    // 透過 REST API 新增節點（WebSocket 協作模式下通常走 CollabController）
    @PostMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<WbsDto.Response> createNode(@PathVariable Long projectId,
            @Valid @RequestBody WbsDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.createNode(projectId, req)));
    }

    // 透過 REST API 更新節點（WebSocket 協作模式下通常走 CollabController）
    @PutMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<WbsDto.Response> updateNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @RequestBody WbsDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.updateNode(projectId, nodeId, req)));
    }

    // 透過 REST API 刪除節點（WebSocket 協作模式下通常走 CollabController）
    @DeleteMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<Void> deleteNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.deleteNode(projectId, nodeId);
        return ApiResponse.ok(null);
    }

    // 前端拖曳排序後批次更新 sortOrder
    @PatchMapping("/api/projects/{projectId}/nodes/reorder")
    public ApiResponse<Void> reorder(@PathVariable Long projectId,
            @RequestBody List<WbsDto.ReorderItem> items,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.reorder(projectId, items);
        return ApiResponse.ok(null);
    }

    // 從模板初始化 WBS，只允許在空專案使用（前端控制，不在 server 端強制）
    @PostMapping("/api/projects/{projectId}/nodes/init")
    public ApiResponse<Void> initFromTemplate(@PathVariable Long projectId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        templateService.applyToProject(body.get("templateId"), projectId);
        return ApiResponse.ok(null);
    }

    // 從 JSON 匯入節點結構，可從其他系統或備份快速恢復 WBS
    @PostMapping("/api/projects/{projectId}/nodes/import-template")
    public ApiResponse<Void> importTemplate(@PathVariable Long projectId,
            @RequestBody String json,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        checkMember(projectId, userDetails);
        templateService.importJson(json, projectId);
        return ApiResponse.ok(null);
    }

    // 需要 canManageSection 才能儲存為模板，防止一般 Member 隨意建立模板
    @PostMapping("/api/projects/{projectId}/save-as-template")
    public ApiResponse<TemplateDto.TemplateResponse> saveAsTemplate(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        // 需要 canManageSection 才能儲存為模板
        if (!user.canManageSection()) {
            throw new SecurityException("只有科長或專案Leader才能儲存模板");
        }
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.saveProjectAsTemplate(projectId, user.getId(), body.get("name"))));
    }

    // 套用模板至指定專案（需有寫入權限）
    @PostMapping("/api/projects/{projectId}/apply-template/{templateId}")
    public ApiResponse<Void> applyTemplate(@PathVariable Long projectId,
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.canWriteProject(projectId, caller)) {
            throw new SecurityException("無編輯權限");
        }
        templateService.applyToProject(templateId, projectId);
        return ApiResponse.ok(null);
    }

    // 讀取：有 canReadProject 才能查看節點（部長也可讀）
    private void checkReadAccess(Long projectId, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.canReadProject(projectId, user)) {
            throw new SecurityException("無存取權限");
        }
    }

    // 寫入：有 canWriteProject 才能修改 WBS（部長被拒絕、歸檔專案被拒絕）
    private void checkMember(Long projectId, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.canWriteProject(projectId, user)) {
            throw new SecurityException("您沒有編輯此專案的權限");
        }
    }
}
