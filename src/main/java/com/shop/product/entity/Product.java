package com.shop.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 封面图 */
    private String coverImg;

    /** 商品图片列表（JSON 数组字符串） */
    private String images;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 状态：1上架 0下架 */
    private Integer status;

    /** 拼团价格（null 表示不支持拼团） */
    private BigDecimal groupPrice;

    /** 拼团需要人数（默认使用全局配置） */
    private Integer groupRequired;

    /** 拼团有效小时数（默认使用全局配置） */
    private Integer groupExpireHours;

    /** 主图（等同 coverImg，供快照使用） */
    public String getMainImage() { return this.coverImg; }

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 乐观锁版本号（防超卖） */
    @Version
    private Integer version;
}
