package com.shop.user.service;

import com.shop.user.dto.LoginDTO;
import com.shop.user.dto.UserInfoDTO;
import com.shop.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 微信小程序登录
     */
    String wxLogin(LoginDTO loginDTO);
    
    /**
     * 获取当前登录用户信息
     */
    UserInfoDTO getCurrentUser();
    
    /**
     * 根据ID获取用户
     */
    User getById(Long id);
    
    /**
     * 更新用户信息
     */
    void updateUserInfo(User user);
}
