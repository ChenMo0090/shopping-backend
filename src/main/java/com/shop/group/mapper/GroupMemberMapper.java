package com.shop.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.group.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /** 查询某拼团的所有成员 */
    @Select("SELECT * FROM t_group_member WHERE group_id = #{groupId} ORDER BY joined_at ASC")
    List<GroupMember> selectByGroupId(@Param("groupId") Long groupId);

    /** 查询某用户是否已加入某拼团 */
    @Select("SELECT * FROM t_group_member WHERE group_id = #{groupId} AND user_id = #{userId} LIMIT 1")
    GroupMember selectByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /** 查询拼团所有成员的订单ID列表 */
    @Select("SELECT order_id FROM t_group_member WHERE group_id = #{groupId}")
    List<Long> selectOrderIdsByGroupId(@Param("groupId") Long groupId);
}
