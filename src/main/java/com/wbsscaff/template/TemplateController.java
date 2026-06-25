package com.wbsscaff.template;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    // 返回 Thymeleaf 靜態殼頁，模板資料由 /api/templates 非同步載入
    @GetMapping("/templates")
    public String templatesPage() { return "template/list"; }

    // 模板節點編輯頁，注入 TEMPLATE_ID 供 Vue 使用
    @GetMapping("/templates/{id}/edit")
    public String templateEditPage(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!user.canManageSection()) return "redirect:/templates";
        TemplateDto.DetailResponse tpl = templateService.getWithNodes(id);
        if (tpl.isSystem()) return "redirect:/templates";
        model.addAttribute("templateId", id);
        model.addAttribute("templateName", tpl.getName());
        return "template/edit";
    }

    // 依角色回傳可見模板：科成員看系統+本科；部長只看系統
    @GetMapping("/api/templates")
    @ResponseBody
    public ApiResponse<TemplateDto.ListResponse> listTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(templateService.listAll(user));
    }

    // 複製系統模板為本科可自訂的版本，保留原始節點結構
    @PostMapping("/api/templates/clone/{templateId}")
    @ResponseBody
    public ApiResponse<TemplateDto.TemplateResponse> cloneTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.cloneSystem(templateId, user.getId())));
    }

    // 只能刪除本科自訂模板，系統模板不可刪除
    @DeleteMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.deleteCustom(id, user);
        return ApiResponse.ok(null);
    }

    // 設定本科預設模板，新增專案時將自動套用此模板
    @PatchMapping("/api/templates/{id}/set-default")
    @ResponseBody
    public ApiResponse<Void> setDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.setDefault(id, user.getId());
        return ApiResponse.ok(null);
    }

    // 取得模板詳細資訊（含節點清單），用於模板管理頁面預覽
    @GetMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<TemplateDto.DetailResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.ok(templateService.getWithNodes(id));
    }

    // 修改本科自訂模板名稱或描述，系統模板不可修改
    @PutMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<TemplateDto.Response> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(templateService.updateTemplate(id, req, caller));
    }

    // 從專案 WBS 快照建立模板，讓科長複用現有專案結構
    @PostMapping("/api/templates/from-project/{projectId}")
    @ResponseBody
    public ApiResponse<TemplateDto.Response> saveFromProject(
            @PathVariable Long projectId,
            @RequestParam String name,
            @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(templateService.saveFromProject(projectId, name, caller));
    }

    // 從頭建立空白自訂模板，不預設任何節點
    @PostMapping("/api/templates")
    @ResponseBody
    public ApiResponse<TemplateDto.Response> createTemplate(
            @RequestBody TemplateDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.Response.from(
            templateService.createEmpty(req.getName(), req.getDescription(), user.getId())));
    }

    // 新增模板節點，parentId 為 null 表示 L1 根節點
    @PostMapping("/api/templates/{id}/nodes")
    @ResponseBody
    public ApiResponse<TemplateDto.NodeResponse> addNode(
            @PathVariable Long id,
            @RequestBody TemplateDto.NodeCreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.NodeResponse.from(
            templateService.addNode(id, req, user.getId())));
    }

    // 更新節點標題與備註（備註僅在 L3 顯示）
    @PutMapping("/api/templates/{id}/nodes/{nodeId}")
    @ResponseBody
    public ApiResponse<TemplateDto.NodeResponse> updateNode(
            @PathVariable Long id, @PathVariable Long nodeId,
            @RequestBody TemplateDto.NodeUpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.NodeResponse.from(
            templateService.updateNode(id, nodeId, req, user.getId())));
    }

    // 刪除節點並遞迴刪除所有子節點
    @DeleteMapping("/api/templates/{id}/nodes/{nodeId}")
    @ResponseBody
    public ApiResponse<Void> deleteNode(
            @PathVariable Long id, @PathVariable Long nodeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.deleteNode(id, nodeId, user.getId());
        return ApiResponse.ok(null);
    }

    // 批次更新拖曳後的排序，前端送完整同層節點的 id+sortOrder 清單
    @PatchMapping("/api/templates/{id}/nodes/reorder")
    @ResponseBody
    public ApiResponse<Void> reorderNodes(
            @PathVariable Long id,
            @RequestBody List<TemplateDto.ReorderItem> items,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.reorderNodes(id, items, user.getId());
        return ApiResponse.ok(null);
    }

    // 匯出為 JSON 供備份或分享給其他科手動匯入，Content-Disposition 觸發瀏覽器下載
    @GetMapping("/api/templates/{id}/export")
    public ResponseEntity<String> exportTemplate(@PathVariable Long id) throws Exception {
        String json = templateService.exportJson(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-" + id + ".json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
            .body(json);
    }
}
