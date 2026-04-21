package com.shop.refund.service;

import com.shop.common.result.PageResult;
import com.shop.refund.entity.Refund;

public interface RefundService {

    /**
     * 分页查询用户退款记录
     */
    PageResult<Refund> listByUser(Long userId, Integer page, Integer size);

    /**
     * 管理员查询退款列表
     */
    PageResult<Refund> listAll(Integer page, Integer size, Integer status);

    /**
     * 处理微信退款回调
     */
    void handleRefundNotify(String outRefundNo, String refundStatus);
}
