package com.shop.product.dto;

import lombok.Data;

/**
 * 商品查询DTO
 */
@Data
public class ProductQueryDTO {
    
    private Long categoryId;
    private String keyword;
    private Integer status = 1;
    private Integer page = 1;
    private Integer size = 10;
}
