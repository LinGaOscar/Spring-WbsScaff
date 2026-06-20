package com.wbsscaff.template;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.UserRepository;
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
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(templateService.listAll(user.getId()));
    }

    @PostMapping("/api/templates/clone/{templateId}")
    @ResponseBody
    public ApiResponse<TemplateDto.TemplateResponse> cloneTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.cloneSystem(templateId, user.getId())));
    }

    @DeleteMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.deleteCustom(id, user.getId());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/templates/{id}/set-default")
    @ResponseBody
    public ApiResponse<Void> setDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.setDefault(id, user.getId());
        return ApiResponse.ok(null);
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
