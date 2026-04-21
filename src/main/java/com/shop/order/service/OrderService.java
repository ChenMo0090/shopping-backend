package com.shop.order.service;

import com.shop.common.result.PageResult;
import com.shop.order.dto.CreateOrderDTO;
import com.shop.order.dto.OrderDTO;
import com.shop.order.dto.OrderQueryDTO;
import com.shop.order.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     */
    OrderDTO createOrder(CreateOrderDTO createDTO);
    
    /**
     * 分页查询我的订单
     */
    PageResult<OrderDTO> listMyOrders(OrderQueryDTO queryDTO);
    
    /**
     * 获取订单详情
     */
    OrderDTO getOrderDetail(Long orderId);
    
    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);
    
    /**
     * 更新订单状态
     */
    void updateOrderStatus(Long orderId, Integer status);
    
    /**
     * 支付成功回调
     */
    void paySuccess(String orderNo, String transactionId);
    
    /**
     * 获取订单实体
     */
    Order getOrderByNo(String orderNo);
    
    /**
     * 获取拼团相关订单
     */
    List<Order> getOrdersByGroupId(Long groupId);
}
