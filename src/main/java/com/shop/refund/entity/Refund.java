package com.shop.refund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体
 * status: 0=处理中 1=退款成功 2=退款失败
 */
@Data
@TableName("t_refund")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号（商户侧，唯一） */
    private String outRefundNo;

    /** 微信退款单号 */
    private String refundId;

    /** 原订单ID */
    private Long orderId;

    /** 申请退款用户ID */
    private Long userId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /**
     * 退款状态
     * 0=处理中, 1=退款成功, 2=退款失败
     */
    private Integer status;

    /** 退款完成时间 */
    private LocalDateTime refundedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 状态常量
    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;
}
