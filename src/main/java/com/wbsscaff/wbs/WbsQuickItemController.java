package com.wbsscaff.wbs;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WbsQuickItemController {

    private final WbsQuickItemRepository quickItemRepository;
    private final UserRepository userRepository;

    // 只有科長或 Leader 才能進入管理頁，其他角色導回專案列表
    @GetMapping("/admin/quick-items")
    public String quickItemsPage(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        // 只有科長或專案Leader才能進入管理頁
        if (!user.canManageSection()) {
            return "redirect:/projects";
        }
        return "admin/quick-items";
    }

    // 科成員看本科 + 全域項目；部長或無所屬科只看全域項目
    @GetMapping("/api/quick-items")
    @ResponseBody
    public ApiResponse<List<WbsQuickItemDto.Response>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        // 科成員看本科 + 全域；部長或無科只看全域
        boolean inSection = user.getDepartment() != null && user.getDepartment().getParent() != null;
        List<WbsQuickItem> items;
        if (inSection) {
            items = quickItemRepository.findBySectionIdOrSectionIsNullOrderBySortOrderAsc(user.getDepartment().getId());
        } else {
            items = quickItemRepository.findBySectionIsNullOrderBySortOrderAsc();
        }
        return ApiResponse.ok(items.stream().map(WbsQuickItemDto.Response::from).toList());
    }

    // 建立的快速子項歸屬建立者的科，不可建立全域項目（全域只由 seed SQL 管理）
    @PostMapping("/api/quick-items")
    @ResponseBody
    public ApiResponse<WbsQuickItemDto.Response> create(
            @Valid @RequestBody WbsQuickItemDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!user.canManageSection()) throw new SecurityException("無管理快速子項的權限");
        if (user.getDepartment() == null) throw new SecurityException("未設定所屬科");
        // 建立的快速子項歸屬建立者的科
        WbsQuickItem item = WbsQuickItem.builder()
            .title(req.getTitle())
            .category(req.getCategory() != null ? req.getCategory() : "常用")
            .sortOrder(req.getSortOrder())
            .requirementDoc(req.getRequirementDoc())
            .section(user.getDepartment())
            .build();
        return ApiResponse.ok(WbsQuickItemDto.Response.from(quickItemRepository.save(item)));
    }

    // 只能修改本科的快速子項，不可修改全域項目（section_id IS NULL）
    @PutMapping("/api/quick-items/{id}")
    @ResponseBody
    public ApiResponse<WbsQuickItemDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody WbsQuickItemDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!user.canManageSection()) throw new SecurityException("無管理快速子項的權限");
        WbsQuickItem item = quickItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("快速子項不存在"));
        // 只能修改本科的快速子項（不可修改全域項目）
        if (item.getSection() == null || !item.getSection().getId().equals(user.getDepartment().getId())) {
            throw new SecurityException("無權修改其他科或全域的快速子項");
        }
        item.setTitle(req.getTitle());
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        item.setRequirementDoc(req.getRequirementDoc());
        return ApiResponse.ok(WbsQuickItemDto.Response.from(quickItemRepository.save(item)));
    }

    // 只能刪除本科的快速子項，全域項目由 SQL migration 管理，不允許透過 API 刪除
    @DeleteMapping("/api/quick-items/{id}")
    @ResponseBody
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!user.canManageSection()) throw new SecurityException("無管理快速子項的權限");
        WbsQuickItem item = quickItemRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("快速子項不存在"));
        // 只能刪除本科的快速子項（不可刪除全域項目）
        if (item.getSection() == null || !item.getSection().getId().equals(user.getDepartment().getId())) {
            throw new SecurityException("無權刪除其他科或全域的快速子項");
        }
        quickItemRepository.deleteById(id);
        return ApiResponse.ok(null);
    }
}
