package com.shop.group.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.R;
import com.shop.group.dto.GroupDetailDTO;
import com.shop.group.dto.GroupJoinDTO;
import com.shop.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "拼团模块")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * 发起拼团
     * 流程：调用此接口 → 返回订单ID → 前端调用 /pay/prepay 完成支付
     * 支付完成后后端回调自动将该用户加入拼团成员
     */
    @Operation(summary = "发起拼团（创建订单，返回拼团码）")
    @PostMapping("/initiate")
    public R<GroupDetailDTO> initiate(@RequestBody GroupJoinDTO dto,
                                      @RequestAttribute("userId") Long userId) {
        return R.ok(groupService.initiateGroup(dto, userId));
    }

    /**
     * 参加拼团
     * 流程：调用此接口 → 返回订单ID → 前端调用 /pay/prepay 完成支付
     */
    @Operation(summary = "参加拼团（创建订单，返回拼团详情）")
    @PostMapping("/join")
    public R<GroupDetailDTO> join(@RequestBody GroupJoinDTO dto,
                                  @RequestAttribute("userId") Long userId) {
        return R.ok(groupService.joinGroup(dto, userId));
    }

    /**
     * 查询拼团详情（无需登录也可访问，但登录后显示个人状态）
     */
    @Operation(summary = "查询拼团详情（含成员列表、倒计时）")
    @GetMapping("/detail/{groupCode}")
    public R<GroupDetailDTO> detail(@PathVariable String groupCode,
                                    @RequestAttribute(value = "userId", required = false) Long userId) {
        return R.ok(groupService.getGroupDetail(groupCode, userId));
    }

    /**
     * 我参与的拼团列表
     */
    @Operation(summary = "我的拼团列表")
    @GetMapping("/my/list")
    public R<PageResult<GroupDetailDTO>> myList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestAttribute("userId") Long userId) {
        return R.ok(groupService.listMyGroups(userId, page, size));
    }
}
