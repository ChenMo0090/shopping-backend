package com.shop.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {
    
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
    
    /**
     * 拼团ID（单独购买不传）
     */
    private Long groupId;
    
    private String remark;
}
