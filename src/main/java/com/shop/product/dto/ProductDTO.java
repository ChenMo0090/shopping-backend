package com.shop.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品DTO
 */
@Data
public class ProductDTO {
    
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String mainImage;
    private String images;
    private BigDecimal originalPrice;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private LocalDateTime createdAt;
}
