package com.shop.order.dto;

import lombok.Data;

/**
 * 订单查询DTO
 */
@Data
public class OrderQueryDTO {
    
    private Integer status;
    private Integer type;
    private Integer page = 1;
    private Integer size = 10;
}
