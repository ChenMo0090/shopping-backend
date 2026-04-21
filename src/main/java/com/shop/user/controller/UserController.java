package com.shop.user.controller;

import com.shop.common.result.R;
import com.shop.user.dto.LoginDTO;
import com.shop.user.dto.UserInfoDTO;
import com.shop.user.entity.User;
import com.shop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 微信小程序登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.wxLogin(loginDTO);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public R<UserInfoDTO> getUserInfo() {
        return R.ok(userService.getCurrentUser());
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public R<Void> updateUserInfo(@RequestBody User user) {
        userService.updateUserInfo(user);
        return R.ok();
    }
}
