package com.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 更新订单状态（含版本校验，防并发）
     */
    @Update("UPDATE t_order SET status = #{newStatus}, updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = #{oldStatus}")
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("oldStatus") int oldStatus,
                     @Param("newStatus") int newStatus);
}
