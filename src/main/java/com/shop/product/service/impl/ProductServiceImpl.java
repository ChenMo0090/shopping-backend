package com.shop.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.result.PageResult;
import com.shop.product.dto.ProductCategoryDTO;
import com.shop.product.dto.ProductDTO;
import com.shop.product.dto.ProductQueryDTO;
import com.shop.product.entity.Product;
import com.shop.product.entity.ProductCategory;
import com.shop.product.mapper.ProductCategoryMapper;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    
    @Override
    public PageResult<ProductDTO> listProducts(ProductQueryDTO queryDTO) {
        Page<Product> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, queryDTO.getStatus());
        
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, queryDTO.getCategoryId());
        }
        
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Product::getName, queryDTO.getKeyword());
        }
        
        wrapper.orderByDesc(Product::getCreatedAt);
        
        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        
        List<ProductDTO> dtoList = productPage.getRecords().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(dtoList, productPage.getTotal(), productPage.getPages());
    }
    
    @Override
    @Cacheable(value = "product", key = "'product:' + #id")
    public ProductDTO getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() != 1) {
            return null;
        }
        return convertToDTO(product);
    }
    
    @Override
    @Cacheable(value = "product", key = "'categories'")
    public List<ProductCategoryDTO> listCategoriesWithProducts() {
        List<ProductCategory> categories = categoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSort)
        );
        
        return categories.stream().map(category -> {
            ProductCategoryDTO dto = new ProductCategoryDTO();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setIcon(category.getIcon());
            dto.setSort(category.getSort());
            
            // 获取该分类下的商品
            List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                    .eq(Product::getCategoryId, category.getId())
                    .eq(Product::getStatus, 1)
                    .orderByDesc(Product::getSales)
                    .last("LIMIT 6")
            );
            
            dto.setProducts(products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public Product getProductEntity(Long id) {
        return productMapper.selectById(id);
    }
    
    @Override
    public boolean decrStock(Long productId, Integer quantity) {
        int rows = productMapper.decrStock(productId, quantity);
        return rows > 0;
    }
    
    @Override
    public void incrStock(Long productId, Integer quantity) {
        productMapper.incrStock(productId, quantity);
    }
    
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        
        // 查询分类名称
        ProductCategory category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }
        
        return dto;
    }
}
