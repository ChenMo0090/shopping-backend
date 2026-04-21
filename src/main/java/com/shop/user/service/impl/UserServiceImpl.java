package com.shop.user.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.interceptor.AuthInterceptor;
import com.shop.common.utils.JwtUtils;
import com.shop.user.dto.LoginDTO;
import com.shop.user.dto.UserInfoDTO;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.shop.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    
    @Value("${shop.wx.app-id}")
    private String appid;

    @Value("${shop.wx.app-secret}")
    private String secret;
    
    @Override
    public String wxLogin(LoginDTO loginDTO) {
        // 调用微信接口获取openid
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, loginDTO.getCode()
        );
        
        String response = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(response);
        
        if (json.containsKey("errcode")) {
            log.error("微信登录失败: {}", response);
            throw new BusinessException("微信登录失败: " + json.getStr("errmsg"));
        }
        
        String openid = json.getStr("openid");
        String sessionKey = json.getStr("session_key");
        
        // 查询或创建用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );
        
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("微信用户" + openid.substring(openid.length() - 6));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        
        // 生成JWT token
        return jwtUtils.generateToken(user.getId());
    }
    
    @Override
    public UserInfoDTO getCurrentUser() {
        Long userId = AuthInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
    
    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Override
    public void updateUserInfo(User user) {
        Long userId = AuthInterceptor.getCurrentUserId();
        user.setId(userId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
