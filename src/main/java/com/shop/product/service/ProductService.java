package com.shop.product.service;

import com.shop.common.result.PageResult;
import com.shop.product.dto.ProductDTO;
import com.shop.product.dto.ProductCategoryDTO;
import com.shop.product.dto.ProductQueryDTO;
import com.shop.product.entity.Product;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {
    
    /**
     * 分页查询商品列表
     */
    PageResult<ProductDTO> listProducts(ProductQueryDTO queryDTO);
    
    /**
     * 根据ID获取商品详情
     */
    ProductDTO getProductById(Long id);
    
    /**
     * 获取商品分类列表（含商品）
     */
    List<ProductCategoryDTO> listCategoriesWithProducts();
    
    /**
     * 获取商品实体（内部使用）
     */
    Product getProductEntity(Long id);
    
    /**
     * 扣减库存（乐观锁）
     */
    boolean decrStock(Long productId, Integer quantity);
    
    /**
     * 增加库存
     */
    void incrStock(Long productId, Integer quantity);
}
