package com.shop.group.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动实体
 * status: 1=进行中 2=已完成（成团） 3=已过期（失败） 4=已关闭
 */
@Data
@TableName("t_group")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拼团码（8位，用于分享链接） */
    private String groupCode;

    /** 关联商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 商品主图（快照） */
    private String productImage;

    /** 拼团价格（快照） */
    private BigDecimal groupPrice;

    /** 发起人用户ID */
    private Long initiatorId;

    /** 发起人订单ID */
    private Long initiatorOrderId;

    /** 需要参团总人数（含发起人） */
    private Integer requiredCount;

    /** 当前已参团人数 */
    private Integer currentCount;

    /**
     * 拼团状态
     * 1=进行中, 2=已完成(成团), 3=已过期(失败), 4=已关闭
     */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ---- 状态常量 ----
    public static final int STATUS_ONGOING   = 1;  // 进行中
    public static final int STATUS_COMPLETED = 2;  // 已完成（成团）
    public static final int STATUS_EXPIRED   = 3;  // 已过期（失败）
    public static final int STATUS_CLOSED    = 4;  // 已关闭
}
