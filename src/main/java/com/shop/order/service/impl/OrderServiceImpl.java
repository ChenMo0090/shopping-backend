package com.shop.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.interceptor.AuthInterceptor;
import com.shop.common.utils.OrderNoUtils;
import com.shop.group.entity.Group;
import com.shop.group.service.GroupService;
import com.shop.order.dto.CreateOrderDTO;
import com.shop.order.dto.OrderDTO;
import com.shop.order.dto.OrderQueryDTO;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.order.service.OrderService;
import com.shop.product.entity.Product;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.shop.common.result.PageResult;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderMapper orderMapper;
    private final ProductService productService;
    private final GroupService groupService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO createOrder(CreateOrderDTO createDTO) {
        Long userId = AuthInterceptor.getCurrentUserId();
        
        // 1. 获取商品信息
        Product product = productService.getProductEntity(createDTO.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        
        // 2. 校验库存
        if (product.getStock() < createDTO.getQuantity()) {
            throw new BusinessException("库存不足");
        }
        
        // 3. 确定订单类型和价格
        Integer orderType = 1; // 1-单独购买
        BigDecimal unitPrice = product.getPrice();
        Long groupId = null;
        
        if (createDTO.getGroupId() != null) {
            // 参团或发起拼团
            Group group = groupService.getGroupById(createDTO.getGroupId());
            if (group == null) {
                throw new BusinessException("拼团活动不存在");
            }
            if (group.getStatus() != 1) {
                throw new BusinessException("拼团活动已结束");
            }
            orderType = 2; // 2-拼团
            unitPrice = group.getGroupPrice();
            groupId = group.getId();
        }
        
        // 4. 扣减库存（乐观锁）
        boolean success = productService.decrStock(product.getId(), createDTO.getQuantity());
        if (!success) {
            throw new BusinessException("库存不足，请刷新重试");
        }
        
        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(OrderNoUtils.generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getMainImage());
        order.setQuantity(createDTO.getQuantity());
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(unitPrice.multiply(new BigDecimal(createDTO.getQuantity())));
        order.setPayAmount(order.getTotalAmount());
        order.setStatus(0); // 0-待支付
        order.setType(orderType);
        order.setGroupId(groupId);
        order.setRemark(createDTO.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        orderMapper.insert(order);
        
        log.info("订单创建成功: orderNo={}, userId={}, amount={}", 
            order.getOrderNo(), userId, order.getPayAmount());
        
        return convertToDTO(order);
    }
    
    @Override
    public PageResult<OrderDTO> listMyOrders(OrderQueryDTO queryDTO) {
        Long userId = AuthInterceptor.getCurrentUserId();
        
        Page<Order> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Order::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(Order::getType, queryDTO.getType());
        }
        
        wrapper.orderByDesc(Order::getCreatedAt);
        
        Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
        
        List<OrderDTO> dtoList = orderPage.getRecords().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(dtoList, orderPage.getTotal(), orderPage.getPages());
    }
    
    @Override
    public OrderDTO getOrderDetail(Long orderId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        return convertToDTO(order);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Long userId = AuthInterceptor.getCurrentUserId();
        
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }
        
        // 恢复库存
        productService.incrStock(order.getProductId(), order.getQuantity());
        
        // 更新订单状态
        order.setStatus(-1); // -1-已取消
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("订单取消成功: orderNo={}", order.getOrderNo());
    }
    
    @Override
    public void updateOrderStatus(Long orderId, Integer status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        orderMapper.updateStatus(orderId, order.getStatus(), status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo, String transactionId) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        
        if (order == null) {
            log.error("支付回调订单不存在: orderNo={}", orderNo);
            return;
        }
        
        if (order.getStatus() != 0) {
            log.warn("订单状态非待支付: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        
        // 更新订单状态
        order.setStatus(1); // 1-已支付
        order.setPayTime(LocalDateTime.now());
        order.setTransactionId(transactionId);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 如果是拼团订单，加入拼团
        if (order.getType() == 2 && order.getGroupId() != null) {
            groupService.joinGroup(order.getGroupId(), order.getId());
        }
        
        log.info("订单支付成功: orderNo={}, transactionId={}", orderNo, transactionId);
    }
    
    @Override
    public Order getOrderByNo(String orderNo) {
        return orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
    }
    
    @Override
    public List<Order> getOrdersByGroupId(Long groupId) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getGroupId, groupId)
                .eq(Order::getStatus, 1) // 已支付
        );
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        BeanUtils.copyProperties(order, dto);
        
        // 状态文本
        dto.setStatusText(getStatusText(order.getStatus()));
        dto.setTypeText(order.getType() == 1 ? "单独购买" : "拼团");
        
        return dto;
    }
    
    private String getStatusText(Integer status) {
        switch (status) {
            case -2: return "已退款";
            case -1: return "已取消";
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "拼团中";
            case 3: return "拼团成功";
            case 4: return "拼团失败";
            case 5: return "已发货";
            case 6: return "已完成";
            default: return "未知";
        }
    }
}
