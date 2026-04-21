package com.shop.product.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.R;
import com.shop.product.dto.ProductCategoryDTO;
import com.shop.product.dto.ProductDTO;
import com.shop.product.dto.ProductQueryDTO;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public R<PageResult<ProductDTO>> listProducts(ProductQueryDTO queryDTO) {
        return R.ok(productService.listProducts(queryDTO));
    }
    
    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public R<ProductDTO> getProduct(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        if (product == null) {
            return R.error("商品不存在或已下架");
        }
        return R.ok(product);
    }
    
    /**
     * 获取分类及商品列表
     */
    @GetMapping("/categories")
    public R<List<ProductCategoryDTO>> listCategories() {
        return R.ok(productService.listCategoriesWithProducts());
    }
}
