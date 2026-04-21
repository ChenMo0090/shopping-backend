package com.shop.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.utils.OrderNoUtils;
import com.shop.order.entity.Order;
import com.shop.order.mapper.OrderMapper;
import com.shop.pay.dto.PayResultDTO;
import com.shop.pay.entity.PayRecord;
import com.shop.pay.mapper.PayRecordMapper;
import com.shop.pay.service.PayService;
import com.shop.refund.entity.Refund;
import com.shop.refund.mapper.RefundMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final OrderMapper orderMapper;
    private final PayRecordMapper payRecordMapper;
    private final RefundMapper refundMapper;
    private final UserMapper userMapper;

    @Value("${shop.wx-pay.app-id}")
    private String appId;
    @Value("${shop.wx-pay.mch-id}")
    private String mchId;
    @Value("${shop.wx-pay.private-key-path}")
    private String privateKeyPath;
    @Value("${shop.wx-pay.merchant-serial-number}")
    private String serialNo;
    @Value("${shop.wx-pay.api-v3-key}")
    private String apiV3Key;
    @Value("${shop.wx-pay.notify-url}")
    private String notifyUrl;

    private Config getConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(serialNo)
                .apiV3Key(apiV3Key)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResultDTO prepay(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作");
        if (order.getStatus() != 0) throw new BusinessException("订单状态异常，无法支付");

        User user = userMapper.selectById(userId);
        if (user == null || user.getOpenid() == null) throw new BusinessException("用户信息异常");

        // 检查是否已有待支付记录
        PayRecord existRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .eq(PayRecord::getStatus, 0)
                .last("LIMIT 1"));

        String outTradeNo;
        if (existRecord != null) {
            outTradeNo = existRecord.getOutTradeNo();
        } else {
            outTradeNo = OrderNoUtils.genPayNo();
            PayRecord record = new PayRecord();
            record.setOrderId(orderId);
            record.setUserId(userId);
            record.setOutTradeNo(outTradeNo);
            record.setAmount(order.getTotalAmount());
            record.setStatus(0);
            payRecordMapper.insert(record);
        }

        // 调用微信支付统一下单
        JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                .config(getConfig()).build();

        PrepayRequest request = new PrepayRequest();
        request.setAppid(appId);
        request.setMchid(mchId);
        request.setDescription(order.getOrderNo());
        request.setOutTradeNo(outTradeNo);
        request.setNotifyUrl(notifyUrl);

        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        // 金额转为分
        amount.setTotal(order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
        request.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(user.getOpenid());
        request.setPayer(payer);

        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

        PayResultDTO result = new PayResultDTO();
        result.setOutTradeNo(outTradeNo);
        result.setTimeStamp(response.getTimeStamp());
        result.setNonceStr(response.getNonceStr());
        result.setPackageVal(response.getPackageVal());
        result.setSignType(response.getSignType());
        result.setPaySign(response.getPaySign());
        result.setStatus(0);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(HttpServletRequest request) {
        try {
            // 读取请求体
            String body = request.getReader().lines().collect(Collectors.joining());
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader("Wechatpay-Serial"))
                    .nonce(request.getHeader("Wechatpay-Nonce"))
                    .signature(request.getHeader("Wechatpay-Signature"))
                    .timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .body(body)
                    .build();

            NotificationConfig notificationConfig = (NotificationConfig) getConfig();
            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            String outTradeNo = transaction.getOutTradeNo();
            String tradeState = transaction.getTradeState().name();
            String transactionId = transaction.getTransactionId();

            if ("SUCCESS".equals(tradeState)) {
                processPaySuccess(outTradeNo, transactionId);
            }

            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        } catch (Exception e) {
            log.error("处理微信支付回调失败", e);
            return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processPaySuccess(String outTradeNo, String transactionId) {
        PayRecord record = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOutTradeNo, outTradeNo));
        if (record == null || record.getStatus() != 0) return; // 防重

        // 更新支付记录
        payRecordMapper.update(null, new LambdaUpdateWrapper<PayRecord>()
                .eq(PayRecord::getOutTradeNo, outTradeNo)
                .set(PayRecord::getStatus, 1)
                .set(PayRecord::getTransactionId, transactionId)
                .set(PayRecord::getPaidAt, LocalDateTime.now()));

        // 更新订单状态：待付款→待发货（或待成团）
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, record.getOrderId())
                .eq(Order::getStatus, 0)
                .set(Order::getStatus, 1)
                .set(Order::getPayTime, LocalDateTime.now()));
    }

    @Override
    public PayResultDTO queryPayStatus(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException("订单不存在");

        PayRecord record = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .orderByDesc(PayRecord::getCreatedAt)
                .last("LIMIT 1"));

        PayResultDTO result = new PayResultDTO();
        if (record != null) {
            result.setOutTradeNo(record.getOutTradeNo());
            result.setStatus(record.getStatus());
        } else {
            result.setStatus(0);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        if (order.getStatus() != 1 && order.getStatus() != 2) throw new BusinessException("当前状态不支持退款");

        PayRecord payRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .eq(PayRecord::getStatus, 1)
                .last("LIMIT 1"));
        if (payRecord == null) throw new BusinessException("支付记录不存在");

        String refundNo = OrderNoUtils.genRefundNo();

        // 调用微信退款接口
        RefundService refundService = new RefundService.Builder().config(getConfig()).build();
        CreateRequest refundRequest = new CreateRequest();
        refundRequest.setOutTradeNo(payRecord.getOutTradeNo());
        refundRequest.setOutRefundNo(refundNo);
        refundRequest.setReason(reason);

        AmountReq amountReq = new AmountReq();
        amountReq.setRefund(order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue());
        amountReq.setTotal(order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue());
        amountReq.setCurrency("CNY");
        refundRequest.setAmount(amountReq);

        com.wechat.pay.java.service.refund.model.Refund wxRefund = refundService.create(refundRequest);

        // 保存退款记录
        com.shop.refund.entity.Refund refund = new com.shop.refund.entity.Refund();
        refund.setOrderId(orderId);
        refund.setUserId(userId);
        refund.setOutRefundNo(refundNo);
        refund.setRefundId(wxRefund.getRefundId());
        refund.setAmount(order.getTotalAmount());
        refund.setReason(reason);
        refund.setStatus(0);
        refundMapper.insert(refund);

        // 更新订单状态为退款中
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, 5));
    }
}
