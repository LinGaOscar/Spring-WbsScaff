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
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    @GetMapping("/templates")
    public String templatesPage() { return "template/list"; }

    @GetMapping("/api/templates")
    @ResponseBody
    public ApiResponse<TemplateDto.ListResponse> listTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(templateService.listAll(user));
    }

    @PostMapping("/api/templates/clone/{templateId}")
    @ResponseBody
    public ApiResponse<TemplateDto.TemplateResponse> cloneTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.cloneSystem(templateId, user.getId())));
    }

    @DeleteMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.deleteCustom(id, user);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/templates/{id}/set-default")
    @ResponseBody
    public ApiResponse<Void> setDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.setDefault(id, user.getId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<TemplateDto.DetailResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.ok(templateService.getWithNodes(id));
    }

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

    // 規格要求的路徑：從專案快照建立模板
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

    @GetMapping("/api/templates/{id}/export")
    public ResponseEntity<String> exportTemplate(@PathVariable Long id) throws Exception {
        String json = templateService.exportJson(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-" + id + ".json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
            .body(json);
    }
}
