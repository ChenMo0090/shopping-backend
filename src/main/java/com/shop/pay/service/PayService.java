package com.shop.pay.service;

import com.shop.pay.dto.PayResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface PayService {

    /**
     * 微信小程序统一下单，返回 JS-SDK 支付参数
     */
    PayResultDTO prepay(Long orderId, Long userId);

    /**
     * 处理微信支付回调通知
     */
    String handleNotify(HttpServletRequest request);

    /**
     * 主动查询微信支付状态（用于轮询兜底）
     */
    PayResultDTO queryPayStatus(Long orderId, Long userId);

    /**
     * 申请退款
     */
    void refund(Long orderId, Long userId, String reason);
}
