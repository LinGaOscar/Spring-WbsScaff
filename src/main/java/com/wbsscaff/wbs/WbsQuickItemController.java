package com.wbsscaff.wbs;

import com.wbsscaff.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WbsQuickItemController {

    private final WbsQuickItemRepository quickItemRepository;

    @GetMapping("/admin/quick-items")
    @PreAuthorize("hasRole('ADMIN')")
    public String quickItemsPage() {
        return "admin/quick-items";
    }

    @GetMapping("/api/quick-items")
    @ResponseBody
    public ApiResponse<List<WbsQuickItemDto.Response>> list() {
        return ApiResponse.ok(quickItemRepository.findAllByOrderBySortOrderAsc()
            .stream().map(WbsQuickItemDto.Response::from).toList());
    }

    @PostMapping("/api/quick-items")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WbsQuickItemDto.Response> create(
            @Valid @RequestBody WbsQuickItemDto.CreateRequest req) {
        WbsQuickItem item = WbsQuickItem.builder()
            .title(req.getTitle())
            .category(req.getCategory() != null ? req.getCategory() : "常用")
            .sortOrder(req.getSortOrder())
            .build();
        return ApiResponse.ok(WbsQuickItemDto.Response.from(quickItemRepository.save(item)));
    }

    @PutMapping("/api/quick-items/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WbsQuickItemDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody WbsQuickItemDto.CreateRequest req) {
        WbsQuickItem item = quickItemRepository.findById(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("快速子項不存在"));
        item.setTitle(req.getTitle());
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        return ApiResponse.ok(WbsQuickItemDto.Response.from(quickItemRepository.save(item)));
    }

    @DeleteMapping("/api/quick-items/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        quickItemRepository.deleteById(id);
        return ApiResponse.ok(null);
    }
}
