package com.shop.group.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼团成员实体
 * role: 0=普通成员 1=发起人
 */
@Data
@TableName("t_group_member")
public class GroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拼团ID */
    private Long groupId;

    /** 参团用户ID */
    private Long userId;

    /** 用户昵称（快照） */
    private String nickname;

    /** 用户头像（快照） */
    private String avatar;

    /** 参团订单ID */
    private Long orderId;

    /**
     * 角色: 0=普通参团者 1=发起人
     */
    private Integer role;

    /** 加入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
