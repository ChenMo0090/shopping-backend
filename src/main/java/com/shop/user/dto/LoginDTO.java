package com.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginDTO {
    
    @NotBlank(message = "code不能为空")
    private String code;
}
