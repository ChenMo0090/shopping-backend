package com.shop.refund.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.R;
import com.shop.refund.entity.Refund;
import com.shop.refund.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "退款模块")
@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "用户退款记录")
    @GetMapping("/list")
    public R<PageResult<Refund>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestAttribute("userId") Long userId) {
        return R.ok(refundService.listByUser(userId, page, size));
    }
}
