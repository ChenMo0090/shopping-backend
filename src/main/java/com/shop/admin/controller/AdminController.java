package com.shop.admin.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.admin.entity.Admin;
import com.shop.admin.mapper.AdminMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.R;
import com.shop.common.utils.JwtUtils;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.product.dto.ProductDTO;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.entity.ProductCategory;
import com.shop.product.mapper.ProductCategoryMapper;
import com.shop.refund.entity.Refund;
import com.shop.refund.service.RefundService;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "管理后台")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final RefundService refundService;
    private final AdminMapper adminMapper;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    // ---- 管理员登录 ----

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest req) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, req.getUsername()));
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtils.generateAdminToken(admin.getId(), admin.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", admin.getId());
        userInfo.put("username", admin.getUsername());
        userInfo.put("name", admin.getName());
        userInfo.put("role", admin.getRole());
        data.put("userInfo", userInfo);
        return R.ok(data);
    }

    // ---- 仪表盘统计 ----

    @Operation(summary = "仪表盘统计数据")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalOrders", orderMapper.selectCount(null));
        data.put("totalProducts", productMapper.selectCount(null));
        data.put("pendingOrders", orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, 0)));
        return R.ok(data);
    }

    // ---- 商品管理 ----

    @Operation(summary = "商品列表（管理员）")
    @GetMapping("/product/list")
    public R<PageResult<Product>> productList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getCreatedAt);
        if (name != null && !name.isBlank()) wrapper.like(Product::getName, name);
        if (status != null) wrapper.eq(Product::getStatus, status);
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(p));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/product/add")
    public R<Void> addProduct(@RequestBody ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(1);
        productMapper.insert(product);
        return R.ok();
    }

    @Operation(summary = "修改商品")
    @PutMapping("/product/{id}")
    public R<Void> updateProduct(@PathVariable Long id, @RequestBody ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setId(id);
        productMapper.updateById(product);
        return R.ok();
    }

    @Operation(summary = "上架/下架商品")
    @PutMapping("/product/{id}/status/{status}")
    public R<Void> toggleProductStatus(@PathVariable Long id, @PathVariable Integer status) {
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id).set(Product::getStatus, status));
        return R.ok();
    }

    @Operation(summary = "删除商品（软删除）")
    @DeleteMapping("/product/{id}")
    public R<Void> deleteProduct(@PathVariable Long id) {
        productMapper.deleteById(id);
        return R.ok();
    }

    // ---- 分类管理 ----

    @Operation(summary = "分类列表")
    @GetMapping("/category/list")
    public R<java.util.List<ProductCategory>> categoryList() {
        return R.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort)));
    }

    @Operation(summary = "新增分类")
    @PostMapping("/category/add")
    public R<Void> addCategory(@RequestBody ProductCategory category) {
        categoryMapper.insert(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (count > 0) throw new BusinessException("该分类下有商品，无法删除");
        categoryMapper.deleteById(id);
        return R.ok();
    }

    // ---- 订单管理 ----

    @Operation(summary = "订单列表（管理员）")
    @GetMapping("/order/list")
    public R<PageResult<Order>> orderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (orderNo != null && !orderNo.isBlank()) wrapper.eq(Order::getOrderNo, orderNo);
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(p));
    }

    @Operation(summary = "订单发货")
    @PutMapping("/order/{id}/ship")
    public R<Void> shipOrder(@PathVariable Long id, @RequestBody ShipDTO dto) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getStatus, 1)
                .set(Order::getStatus, 2)
                .set(Order::getShipCompany, dto.getShipCompany())
                .set(Order::getShipNo, dto.getShipNo()));
        return R.ok();
    }

    // ---- 用户管理 ----

    @Operation(summary = "用户列表")
    @GetMapping("/user/list")
    public R<PageResult<User>> userList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<User> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return R.ok(PageResult.of(p));
    }

    // ---- 退款管理 ----

    @Operation(summary = "退款列表（管理员）")
    @GetMapping("/refund/list")
    public R<PageResult<Refund>> refundList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return R.ok(refundService.listAll(page, size, status));
    }

    @Data
    public static class ShipDTO {
        private String shipCompany;
        private String shipNo;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
