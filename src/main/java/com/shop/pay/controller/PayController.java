package com.shop.pay.controller;

import com.shop.common.result.R;
import com.shop.pay.dto.PrepayDTO;
import com.shop.pay.dto.PayResultDTO;
import com.shop.pay.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付模块")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @Operation(summary = "发起支付（统一下单）")
    @PostMapping("/prepay")
    public R<PayResultDTO> prepay(@RequestBody PrepayDTO dto,
                                  @RequestAttribute("userId") Long userId) {
        return R.ok(payService.prepay(dto.getOrderId(), userId));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/status/{orderId}")
    public R<PayResultDTO> queryStatus(@PathVariable Long orderId,
                                       @RequestAttribute("userId") Long userId) {
        return R.ok(payService.queryPayStatus(orderId, userId));
    }

    @Operation(summary = "申请退款")
    @PostMapping("/refund/{orderId}")
    public R<Void> refund(@PathVariable Long orderId,
                          @RequestParam(defaultValue = "用户申请退款") String reason,
                          @RequestAttribute("userId") Long userId) {
        payService.refund(orderId, userId, reason);
        return R.ok();
    }

    /**
     * 微信支付回调 —— 不需要 JWT 鉴权，需在拦截器白名单中放行
     */
    @Operation(summary = "微信支付回调通知（内部接口）")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        return payService.handleNotify(request);
    }
}
