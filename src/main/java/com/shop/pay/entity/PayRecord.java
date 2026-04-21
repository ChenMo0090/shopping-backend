package com.shop.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 * status: 0=待支付 1=已支付 2=已退款
 */
@Data
@TableName("t_pay_record")
public class PayRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 商户支付单号（唯一） */
    private String outTradeNo;

    /** 微信交易号（回调后填入） */
    private String transactionId;

    /** 支付金额（元） */
    private BigDecimal amount;

    /**
     * 状态: 0=待支付 1=已支付 2=已退款
     */
    private Integer status;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
