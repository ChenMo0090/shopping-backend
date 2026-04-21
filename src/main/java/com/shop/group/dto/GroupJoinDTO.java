package com.shop.group.dto;

import lombok.Data;

/**
 * 发起/参加拼团请求DTO
 */
@Data
public class GroupJoinDTO {

    /** 商品ID（发起新拼团时必填） */
    private Long productId;

    /** 商品数量，默认1 */
    private Integer quantity = 1;

    /**
     * 拼团码（参加已有拼团时必填；为空则发起新拼团）
     */
    private String groupCode;

    /** 收货地址ID */
    private Long addressId;

    /** 备注 */
    private String remark;
}
