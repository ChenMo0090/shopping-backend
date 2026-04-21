package com.shop.order.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.R;
import com.shop.order.dto.CreateOrderDTO;
import com.shop.order.dto.OrderDTO;
import com.shop.order.dto.OrderQueryDTO;
import com.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 创建订单
     */
    @PostMapping("/create")
    public R<OrderDTO> createOrder(@Valid @RequestBody CreateOrderDTO createDTO) {
        return R.ok(orderService.createOrder(createDTO));
    }
    
    /**
     * 获取订单列表
     */
    @GetMapping("/list")
    public R<PageResult<OrderDTO>> listOrders(OrderQueryDTO queryDTO) {
        return R.ok(orderService.listMyOrders(queryDTO));
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public R<OrderDTO> getOrder(@PathVariable Long id) {
        return R.ok(orderService.getOrderDetail(id));
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return R.ok();
    }
}
