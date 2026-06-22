package com.wbsscaff.user;

import com.wbsscaff.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/api/users/me")
    @ResponseBody
    public ApiResponse<UserDto.Response> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        // 取得目前登入使用者資訊，用於前端判斷是否顯示「建立專案」按鈕
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        return ApiResponse.ok(UserDto.Response.from(user));
    }

    @GetMapping("/api/users")
    @ResponseBody
    public ApiResponse<List<UserDto.Response>> listUsers() {
        // 提供給專案成員指派頁面使用（列出可指派的使用者）
        return ApiResponse.ok(userService.listUsers().stream()
            .map(UserDto.Response::from).toList());
    }
}
