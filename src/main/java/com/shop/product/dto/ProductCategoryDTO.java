package com.shop.product.dto;

import lombok.Data;

import java.util.List;

/**
 * 商品分类DTO
 */
@Data
public class ProductCategoryDTO {
    
    private Long id;
    private String name;
    private String icon;
    private Integer sort;
    private List<ProductDTO> products;
}
