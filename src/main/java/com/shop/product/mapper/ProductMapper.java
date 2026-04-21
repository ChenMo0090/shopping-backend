package com.shop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 安全减库存（带乐观锁防超卖）
     */
    @Update("UPDATE t_product SET stock = stock - #{qty}, sales = sales + #{qty} " +
            "WHERE id = #{productId} AND stock >= #{qty}")
    int decrStock(@org.apache.ibatis.annotations.Param("productId") Long productId,
                  @org.apache.ibatis.annotations.Param("qty") int qty);

    /**
     * 回滚库存
     */
    @Update("UPDATE t_product SET stock = stock + #{qty} WHERE id = #{productId}")
    int incrStock(@org.apache.ibatis.annotations.Param("productId") Long productId,
                  @org.apache.ibatis.annotations.Param("qty") int qty);
}
