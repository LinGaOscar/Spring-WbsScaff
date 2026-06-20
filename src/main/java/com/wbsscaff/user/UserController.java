package com.wbsscaff.user;

import com.wbsscaff.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
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
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String usersPage() {
        return "admin/users";
    }

    @GetMapping("/api/users/me")
    @ResponseBody
    public ApiResponse<UserDto.Response> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        // 取得目前登入使用者資訊，用於 Vue 前端判斷是否顯示「建立專案」按鈕
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(UserDto.Response.from(user));
    }

    @GetMapping("/api/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDto.Response>> listUsers() {
        return ApiResponse.ok(userService.listUsers().stream()
            .map(UserDto.Response::from).toList());
    }

    @PostMapping("/api/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto.Response> createUser(@Valid @RequestBody UserDto.CreateRequest req) {
        return ApiResponse.ok(UserDto.Response.from(userService.createUser(req)));
    }

    @PutMapping("/api/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto.Response> updateUser(
            @PathVariable Long id, @RequestBody UserDto.UpdateRequest req) {
        return ApiResponse.ok(UserDto.Response.from(userService.updateUser(id, req)));
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/users/{id}/can-create-project")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> setCanCreateProject(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        userService.setCanCreateProject(id, body.get("value"));
        return ApiResponse.ok(null);
    }
}
