package com.shop.refund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.result.PageResult;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.refund.entity.Refund;
import com.shop.refund.mapper.RefundMapper;
import com.shop.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;

    @Override
    public PageResult<Refund> listByUser(Long userId, Integer page, Integer size) {
        Page<Refund> p = refundMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getUserId, userId)
                        .orderByDesc(Refund::getCreatedAt));
        return PageResult.of(p);
    }

    @Override
    public PageResult<Refund> listAll(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<Refund>()
                .orderByDesc(Refund::getCreatedAt);
        if (status != null) wrapper.eq(Refund::getStatus, status);
        Page<Refund> p = refundMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundNotify(String outRefundNo, String refundStatus) {
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOutRefundNo, outRefundNo));
        if (refund == null) return;

        int newStatus = "SUCCESS".equals(refundStatus) ? 1 : 2; // 1=退款成功 2=退款失败
        refundMapper.update(null, new LambdaUpdateWrapper<Refund>()
                .eq(Refund::getOutRefundNo, outRefundNo)
                .set(Refund::getStatus, newStatus)
                .set(Refund::getRefundedAt, LocalDateTime.now()));

        if (newStatus == 1) {
            // 更新订单状态为已退款
            orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, refund.getOrderId())
                    .set(Order::getStatus, 6));
        }
    }
}
