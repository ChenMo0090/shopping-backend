package com.shop.group.service;

import com.shop.common.result.PageResult;
import com.shop.group.dto.GroupDetailDTO;
import com.shop.group.dto.GroupJoinDTO;
import com.shop.group.entity.Group;

/**
 * 拼团核心服务接口
 */
public interface GroupService {

    /**
     * 发起新拼团（同时创建订单，返回拼团详情）
     * @param dto     请求参数（productId 必填，groupCode 为空）
     * @param userId  当前登录用户ID
     * @return 新拼团详情（含拼团码、订单ID）
     */
    GroupDetailDTO initiateGroup(GroupJoinDTO dto, Long userId);

    /**
     * 参加已有拼团（同时创建订单）
     * @param dto     请求参数（groupCode 必填）
     * @param userId  当前登录用户ID
     * @return 拼团详情（若参团后达到满员则立即触发成团逻辑）
     */
    GroupDetailDTO joinGroup(GroupJoinDTO dto, Long userId);

    /**
     * 支付成功后回调入团（由 OrderServiceImpl.paySuccess 调用）
     * @param groupId  拼团ID
     * @param orderId  已支付订单ID
     */
    void joinGroup(Long groupId, Long orderId);

    /**
     * 发起拼团（无DTO重载，供 OrderServiceImpl 调用）
     */
    Group initiateGroup(Long productId, Long userId, Long orderId);

    /**
     * 查询拼团详情（含成员列表、倒计时等）
     */
    GroupDetailDTO getGroupDetail(String groupCode, Long userId);

    /**
     * 根据ID查询拼团（内部调用）
     */
    Group getGroupById(Long groupId);

    /**
     * 拼团成功处理：更新状态、更新所有成员订单为待发货
     */
    void handleGroupSuccess(Long groupId);

    /**
     * 拼团失败处理：更新状态、触发所有成员退款
     */
    void handleGroupFail(Long groupId);

    /**
     * 分页查询我参与的拼团
     */
    PageResult<GroupDetailDTO> listMyGroups(Long userId, Integer page, Integer size);
}
