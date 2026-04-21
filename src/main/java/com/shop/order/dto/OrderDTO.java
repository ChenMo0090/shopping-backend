package com.shop.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单DTO
 */
@Data
public class OrderDTO {
    
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer status;
    private String statusText;
    private Integer type;
    private String typeText;
    private Long groupId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime payTime;
}
