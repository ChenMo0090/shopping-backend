package com.shop.scheduler;

import com.shop.group.entity.Group;
import com.shop.group.mapper.GroupMapper;
import com.shop.group.service.GroupService;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拼团定时任务
 * <p>
 * 两类任务：
 * 1. 每分钟扫描已过期的进行中拼团 → 触发拼团失败流程（退款 + 恢复库存）
 * 2. 每5分钟扫描超15分钟未付款的待付款订单 → 自动取消（恢复库存）
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupScheduler {

    private final GroupMapper groupMapper;
    private final GroupService groupService;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    /**
     * 每分钟执行一次：处理超时未成团的拼团
     */
    @Scheduled(fixedDelay = 60_000)
    public void handleExpiredGroups() {
        List<Group> expiredGroups = groupMapper.selectExpiredOngoing();
        if (expiredGroups.isEmpty()) return;

        log.info("[定时任务] 发现 {} 个过期拼团，开始处理...", expiredGroups.size());
        for (Group group : expiredGroups) {
            try {
                groupService.handleGroupFail(group.getId());
                log.info("[定时任务] 拼团失败处理完成: groupId={}, groupCode={}",
                        group.getId(), group.getGroupCode());
            } catch (Exception e) {
                log.error("[定时任务] 处理过期拼团异常: groupId={}, error={}",
                        group.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 每5分钟执行一次：取消超时未支付的拼团订单（超过30分钟）
     */
    @Scheduled(fixedDelay = 300_000)
    public void cancelUnpaidGroupOrders() {
        // 查询超过30分钟未支付的拼团订单
        List<Order> unpaidOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, Order.STATUS_PENDING_PAY)
                        .eq(Order::getIsGroup, 1)
                        .lt(Order::getCreatedAt,
                                java.time.LocalDateTime.now().minusMinutes(30)));

        if (unpaidOrders.isEmpty()) return;

        log.info("[定时任务] 发现 {} 个超时未付款拼团订单，开始取消...", unpaidOrders.size());
        for (Order order : unpaidOrders) {
            try {
                // 恢复库存
                productMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.shop.product.entity.Product>()
                                .eq(com.shop.product.entity.Product::getId, order.getProductId())
                                .setSql("stock = stock + " + (order.getQuantity() == null ? 1 : order.getQuantity())));

                // 取消订单
                orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                                .eq(Order::getId, order.getId())
                                .eq(Order::getStatus, Order.STATUS_PENDING_PAY)
                                .set(Order::getStatus, Order.STATUS_CANCELLED));

                log.info("[定时任务] 超时未付款订单已取消: orderId={}", order.getId());
            } catch (Exception e) {
                log.error("[定时任务] 取消超时订单异常: orderId={}, error={}",
                        order.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 每天凌晨2点：自动确认收货（发货超过15天未确认）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoConfirmReceived() {
        List<Order> shippedOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, Order.STATUS_SHIPPED)
                        .lt(Order::getShippedAt,
                                java.time.LocalDateTime.now().minusDays(15)));

        if (shippedOrders.isEmpty()) return;

        log.info("[定时任务] 自动确认收货: {} 个订单", shippedOrders.size());
        for (Order order : shippedOrders) {
            orderMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                            .eq(Order::getId, order.getId())
                            .eq(Order::getStatus, Order.STATUS_SHIPPED)
                            .set(Order::getStatus, Order.STATUS_COMPLETED)
                            .set(Order::getReceivedAt, java.time.LocalDateTime.now()));
        }
    }
}
