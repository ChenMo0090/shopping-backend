package com.shop.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.utils.OrderNoUtils;
import com.shop.group.dto.GroupDetailDTO;
import com.shop.group.dto.GroupJoinDTO;
import com.shop.group.entity.Group;
import com.shop.group.entity.GroupMember;
import com.shop.group.mapper.GroupMapper;
import com.shop.group.mapper.GroupMemberMapper;
import com.shop.group.service.GroupService;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.pay.service.PayService;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    // 使用 @Lazy 打破循环依赖（GroupService ↔ PayService）
    @Lazy
    private final PayService payService;

    /** 拼团有效时长（小时），从配置读取，默认24h */
    @Value("${shop.group.expire-hours:24}")
    private int groupExpireHours;

    /** 每个拼团所需人数，从配置读取，默认3人 */
    @Value("${shop.group.required-count:3}")
    private int defaultRequiredCount;

    // =====================================================================
    //  发起拼团
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupDetailDTO initiateGroup(GroupJoinDTO dto, Long userId) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (product.getGroupPrice() == null) {
            throw new BusinessException("该商品不支持拼团");
        }
        if (product.getStock() < dto.getQuantity()) {
            throw new BusinessException("商品库存不足");
        }

        // 检查用户是否已参与该商品的进行中拼团
        checkAlreadyJoined(userId, dto.getProductId());

        // 扣减库存
        int affected = productMapper.decrStock(product.getId(), dto.getQuantity());
        if (affected == 0) throw new BusinessException("库存不足，请刷新重试");

        // 创建订单（待付款）
        Order order = buildOrder(userId, product, dto, null, product.getGroupPrice());
        orderMapper.insert(order);

        // 创建拼团
        User user = userMapper.selectById(userId);
        Group group = new Group();
        group.setGroupCode(OrderNoUtils.generateGroupCode());
        group.setProductId(product.getId());
        group.setProductName(product.getName());
        group.setProductImage(product.getMainImage());
        group.setGroupPrice(product.getGroupPrice());
        group.setInitiatorId(userId);
        group.setInitiatorOrderId(order.getId());
        group.setRequiredCount(defaultRequiredCount);
        group.setCurrentCount(1);
        group.setStatus(Group.STATUS_ONGOING);
        group.setExpireAt(LocalDateTime.now().plusHours(groupExpireHours));
        groupMapper.insert(group);

        // 更新订单关联的拼团ID
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .set(Order::getGroupId, group.getId())
                .set(Order::getIsGroup, 1));

        // 创建拼团成员记录（发起人，角色=1）
        GroupMember member = buildMember(group.getId(), userId, order.getId(), user, 1);
        groupMemberMapper.insert(member);

        log.info("拼团发起成功: groupCode={}, userId={}, productId={}",
                group.getGroupCode(), userId, product.getId());

        return buildDetailDTO(group, userId);
    }

    // =====================================================================
    //  参加拼团（DTO 版）
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupDetailDTO joinGroup(GroupJoinDTO dto, Long userId) {
        if (dto.getGroupCode() == null || dto.getGroupCode().isBlank()) {
            throw new BusinessException("拼团码不能为空");
        }

        Group group = groupMapper.selectByGroupCode(dto.getGroupCode());
        if (group == null) throw new BusinessException("拼团不存在");
        if (group.getStatus() != Group.STATUS_ONGOING) throw new BusinessException("拼团已结束");
        if (LocalDateTime.now().isAfter(group.getExpireAt())) throw new BusinessException("拼团已过期");

        // 不能参与自己已经在的拼团
        GroupMember existMember = groupMemberMapper.selectByGroupIdAndUserId(group.getId(), userId);
        if (existMember != null) throw new BusinessException("您已参与该拼团");

        Product product = productMapper.selectById(group.getProductId());
        if (product == null || product.getStatus() != 1) throw new BusinessException("商品已下架");
        if (product.getStock() < 1) throw new BusinessException("商品库存不足");

        // 扣减库存
        int affected = productMapper.decrStock(product.getId(), 1);
        if (affected == 0) throw new BusinessException("库存不足，请刷新重试");

        // 原子递增参团人数（防并发超额）
        int rows = groupMapper.incrMemberCount(group.getId());
        if (rows == 0) throw new BusinessException("拼团已满员，请发起新拼团");

        // 创建订单（待付款）
        Order order = buildOrder(userId, product, null, group.getId(), group.getGroupPrice());
        order.setIsGroup(1);
        orderMapper.insert(order);

        // 创建成员记录（角色=0，普通参团者）
        User user = userMapper.selectById(userId);
        GroupMember member = buildMember(group.getId(), userId, order.getId(), user, 0);
        groupMemberMapper.insert(member);

        log.info("参团成功: groupCode={}, userId={}, orderId={}",
                dto.getGroupCode(), userId, order.getId());

        // 重新查询最新人数（incrMemberCount 已+1，重新读取确认状态）
        group = groupMapper.selectById(group.getId());
        return buildDetailDTO(group, userId);
    }

    // =====================================================================
    //  支付成功后回调入团（OrderServiceImpl.paySuccess 调用）
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long groupId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != Group.STATUS_ONGOING) return;

        // 检查是否已记录（幂等保护）
        GroupMember exist = groupMemberMapper.selectByGroupIdAndUserId(groupId, order.getUserId());
        if (exist != null) {
            // 已存在则仅检查是否需要触发成团
            checkAndTriggerSuccess(group);
            return;
        }

        User user = userMapper.selectById(order.getUserId());
        int role = group.getInitiatorId().equals(order.getUserId()) ? 1 : 0;
        GroupMember member = buildMember(groupId, order.getUserId(), orderId, user, role);
        groupMemberMapper.insert(member);

        // 原子递增
        groupMapper.incrMemberCount(groupId);

        // 重新查最新状态
        group = groupMapper.selectById(groupId);
        checkAndTriggerSuccess(group);
    }

    // =====================================================================
    //  供 OrderServiceImpl 调用的重载（直接发起）
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Group initiateGroup(Long productId, Long userId, Long orderId) {
        Product product = productMapper.selectById(productId);
        User user = userMapper.selectById(userId);

        Group group = new Group();
        group.setGroupCode(OrderNoUtils.generateGroupCode());
        group.setProductId(productId);
        group.setProductName(product.getName());
        group.setProductImage(product.getMainImage());
        group.setGroupPrice(product.getGroupPrice());
        group.setInitiatorId(userId);
        group.setInitiatorOrderId(orderId);
        group.setRequiredCount(defaultRequiredCount);
        group.setCurrentCount(1);
        group.setStatus(Group.STATUS_ONGOING);
        group.setExpireAt(LocalDateTime.now().plusHours(groupExpireHours));
        groupMapper.insert(group);

        GroupMember member = buildMember(group.getId(), userId, orderId, user, 1);
        groupMemberMapper.insert(member);

        return group;
    }

    // =====================================================================
    //  查询拼团详情
    // =====================================================================

    @Override
    public GroupDetailDTO getGroupDetail(String groupCode, Long userId) {
        Group group = groupMapper.selectByGroupCode(groupCode);
        if (group == null) throw new BusinessException("拼团不存在");
        return buildDetailDTO(group, userId);
    }

    @Override
    public Group getGroupById(Long groupId) {
        return groupMapper.selectById(groupId);
    }

    // =====================================================================
    //  拼团成功处理（满员时触发）
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleGroupSuccess(Long groupId) {
        // CAS 更新状态，防并发重复触发
        int rows = groupMapper.update(null, new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .eq(Group::getStatus, Group.STATUS_ONGOING)
                .set(Group::getStatus, Group.STATUS_COMPLETED)
                .set(Group::getCompletedAt, LocalDateTime.now()));

        if (rows == 0) {
            log.warn("拼团成功状态更新失败（可能已处理）: groupId={}", groupId);
            return;
        }

        // 将所有成员订单状态从「待付款/已付款」→ 「待发货（已付款）」
        List<Long> orderIds = groupMemberMapper.selectOrderIdsByGroupId(groupId);
        for (Long orderId : orderIds) {
            orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, orderId)
                    .eq(Order::getStatus, Order.STATUS_PAID)
                    .set(Order::getStatus, Order.STATUS_PAID)); // 保持已付款，等待管理员发货
        }

        log.info("拼团成功: groupId={}, 成员订单数={}", groupId, orderIds.size());
    }

    // =====================================================================
    //  拼团失败处理（超时/关闭时触发）
    // =====================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleGroupFail(Long groupId) {
        // CAS 更新状态
        int rows = groupMapper.update(null, new LambdaUpdateWrapper<Group>()
                .eq(Group::getId, groupId)
                .eq(Group::getStatus, Group.STATUS_ONGOING)
                .set(Group::getStatus, Group.STATUS_EXPIRED));

        if (rows == 0) {
            log.warn("拼团失败状态更新失败（可能已处理）: groupId={}", groupId);
            return;
        }

        // 恢复所有成员的库存并触发退款（只对已付款订单退款）
        Group group = groupMapper.selectById(groupId);
        List<Long> orderIds = groupMemberMapper.selectOrderIdsByGroupId(groupId);

        for (Long orderId : orderIds) {
            Order order = orderMapper.selectById(orderId);
            if (order == null) continue;

            // 恢复库存
            productMapper.update(null, new LambdaUpdateWrapper<com.shop.product.entity.Product>()
                    .eq(com.shop.product.entity.Product::getId, order.getProductId())
                    .setSql("stock = stock + " + (order.getQuantity() == null ? 1 : order.getQuantity())));

            if (order.getStatus() == Order.STATUS_PAID) {
                // 已付款 → 触发退款
                try {
                    payService.refund(orderId, order.getUserId(), "拼团失败自动退款");
                } catch (Exception e) {
                    log.error("拼团失败退款异常: orderId={}, error={}", orderId, e.getMessage());
                }
            } else if (order.getStatus() == Order.STATUS_PENDING_PAY) {
                // 未付款 → 直接取消
                orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, Order.STATUS_PENDING_PAY)
                        .set(Order::getStatus, Order.STATUS_CANCELLED));
            }
        }

        log.info("拼团失败处理完成: groupId={}, 涉及订单数={}", groupId, orderIds.size());
    }

    // =====================================================================
    //  我参与的拼团列表
    // =====================================================================

    @Override
    public PageResult<GroupDetailDTO> listMyGroups(Long userId, Integer page, Integer size) {
        // 先查用户参与的拼团成员记录
        Page<GroupMember> memberPage = groupMemberMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getUserId, userId)
                        .orderByDesc(GroupMember::getJoinedAt));

        List<GroupDetailDTO> list = memberPage.getRecords().stream()
                .map(m -> {
                    Group g = groupMapper.selectById(m.getGroupId());
                    return g == null ? null : buildDetailDTO(g, userId);
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());

        PageResult<GroupDetailDTO> result = new PageResult<>();
        result.setTotal(memberPage.getTotal());
        result.setCurrent(memberPage.getCurrent());
        result.setSize(memberPage.getSize());
        result.setRecords(list);
        return result;
    }

    // =====================================================================
    //  私有辅助方法
    // =====================================================================

    /** 检查是否满员，满员则触发成功 */
    private void checkAndTriggerSuccess(Group group) {
        if (group.getCurrentCount() >= group.getRequiredCount()) {
            handleGroupSuccess(group.getId());
        }
    }

    /** 检查用户是否已参与同一商品的进行中拼团 */
    private void checkAlreadyJoined(Long userId, Long productId) {
        List<GroupMember> myMembers = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUserId, userId));
        for (GroupMember m : myMembers) {
            Group g = groupMapper.selectById(m.getGroupId());
            if (g != null && g.getStatus() == Group.STATUS_ONGOING
                    && g.getProductId().equals(productId)) {
                throw new BusinessException("您已在该商品的拼团中，请勿重复发起");
            }
        }
    }

    /** 构建订单对象 */
    private Order buildOrder(Long userId, Product product, GroupJoinDTO dto,
                             Long groupId, BigDecimal unitPrice) {
        int qty = (dto != null && dto.getQuantity() != null) ? dto.getQuantity() : 1;
        Order order = new Order();
        order.setOrderNo(OrderNoUtils.generateOrderNo());
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setQuantity(qty);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(qty)));
        order.setPayAmount(order.getTotalAmount());
        order.setStatus(Order.STATUS_PENDING_PAY);
        order.setGroupId(groupId);
        order.setIsGroup(1);
        if (dto != null) order.setRemark(dto.getRemark());
        return order;
    }

    /** 构建成员记录 */
    private GroupMember buildMember(Long groupId, Long userId, Long orderId,
                                    User user, int role) {
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setOrderId(orderId);
        member.setRole(role);
        member.setNickname(user != null ? user.getNickname() : "");
        member.setAvatar(user != null ? user.getAvatarUrl() : "");
        member.setJoinedAt(LocalDateTime.now());
        return member;
    }

    /** 将 Group 实体转换为 DTO，注入成员列表和当前用户状态 */
    private GroupDetailDTO buildDetailDTO(Group group, Long userId) {
        GroupDetailDTO dto = new GroupDetailDTO();
        dto.setId(group.getId());
        dto.setGroupCode(group.getGroupCode());
        dto.setProductId(group.getProductId());
        dto.setProductName(group.getProductName());
        dto.setProductImage(group.getProductImage());
        dto.setGroupPrice(group.getGroupPrice());
        dto.setRequiredCount(group.getRequiredCount());
        dto.setCurrentCount(group.getCurrentCount());
        dto.setNeedCount(Math.max(0, group.getRequiredCount() - group.getCurrentCount()));
        dto.setStatus(group.getStatus());
        dto.setExpireAt(group.getExpireAt());

        // 状态文本
        switch (group.getStatus()) {
            case Group.STATUS_ONGOING   -> dto.setStatusText("拼团中");
            case Group.STATUS_COMPLETED -> dto.setStatusText("拼团成功");
            case Group.STATUS_EXPIRED   -> dto.setStatusText("拼团失败");
            default                     -> dto.setStatusText("已关闭");
        }

        // 剩余秒数
        if (group.getStatus() == Group.STATUS_ONGOING && group.getExpireAt() != null) {
            long remain = group.getExpireAt().toEpochSecond(ZoneOffset.of("+8"))
                    - LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
            dto.setRemainSeconds(Math.max(0, remain));
        }

        // 成员列表
        List<GroupMember> members = groupMemberMapper.selectByGroupId(group.getId());
        dto.setMembers(members);

        // 当前用户是否已参团
        GroupMember myRecord = null;
        if (userId != null) {
            myRecord = members.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst().orElse(null);
        }
        dto.setJoined(myRecord != null);
        dto.setMyOrderId(myRecord != null ? myRecord.getOrderId() : null);

        return dto;
    }
}
