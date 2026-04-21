package com.shop.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付请求DTO
 */
@Data
public class PayRequestDTO {
    
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}
