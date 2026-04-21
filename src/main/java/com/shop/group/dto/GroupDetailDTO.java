package com.shop.group.dto;

import com.shop.group.entity.GroupMember;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团详情响应DTO
 */
@Data
public class GroupDetailDTO {

    private Long id;
    private String groupCode;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal groupPrice;

    /** 需要参团总人数 */
    private Integer requiredCount;
    /** 当前已参团人数 */
    private Integer currentCount;
    /** 还需人数 */
    private Integer needCount;

    /**
     * 状态: 1=进行中 2=已完成 3=已过期
     */
    private Integer status;
    private String statusText;

    /** 过期时间 */
    private LocalDateTime expireAt;
    /** 剩余秒数（进行中时有效） */
    private Long remainSeconds;

    /** 成员列表 */
    private List<GroupMember> members;

    /** 当前用户是否已参团 */
    private Boolean joined;

    /** 当前用户的订单ID（已参团时有值） */
    private Long myOrderId;
}
