package com.shop.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
public class UserInfoDTO {
    
    private Long id;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
}
