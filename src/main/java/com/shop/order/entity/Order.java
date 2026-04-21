package com.shop.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * status: 0待付款 1待发货/待成团 2已发货 3已完成 4申请退款 5退款中 6已退款 7已取消
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称快照 */
    private String productName;

    /** 商品图片快照 */
    private String productImage;

    /** 商品数量 */
    private Integer quantity;

    /** 单价快照 */
    private BigDecimal unitPrice;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 订单类型：1=普通购买 2=拼团 */
    private Integer type;

    /**
     * 订单状态
     * 0=待付款, 1=待发货/待成团, 2=已发货, 3=已完成,
     * 4=申请退款, 5=退款中, 6=已退款, 7=已取消
     */
    private Integer status;

    /** 收货地址快照（JSON） */
    private String addressInfo;

    /** 关联拼团ID（普通订单为null） */
    private Long groupId;

    /** 是否拼团订单 0=否 1=是 */
    private Integer isGroup;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 微信支付交易号 */
    private String transactionId;

    /** 物流公司 */
    private String shipCompany;

    /** 物流单号 */
    private String shipNo;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 确认收货时间 */
    private LocalDateTime receivedAt;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 状态常量
    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_SHIPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    public static final int STATUS_REFUND_APPLYING = 4;
    public static final int STATUS_REFUNDING = 5;
    public static final int STATUS_REFUNDED = 6;
    public static final int STATUS_CANCELLED = 7;
}
