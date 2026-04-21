package com.shop.group.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.group.entity.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {

    /**
     * 原子递增参团人数（仅在状态=进行中且未满员时生效）
     * 返回受影响行数：1=成功 0=已满/不存在/状态非法
     */
    @Update("UPDATE t_group SET current_count = current_count + 1 " +
            "WHERE id = #{groupId} AND status = 1 AND current_count < required_count")
    int incrMemberCount(@Param("groupId") Long groupId);

    /**
     * 查询所有已过期但状态仍为进行中的拼团（供定时任务使用）
     */
    @Select("SELECT * FROM t_group WHERE status = 1 AND expire_at < NOW()")
    List<Group> selectExpiredOngoing();

    /**
     * 根据拼团码查询
     */
    @Select("SELECT * FROM t_group WHERE group_code = #{groupCode} LIMIT 1")
    Group selectByGroupCode(@Param("groupCode") String groupCode);
}
