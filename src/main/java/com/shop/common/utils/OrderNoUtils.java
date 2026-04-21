package com.shop.common.utils;

import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号 / 支付单号 / 拼团码 生成工具类
 */
public class OrderNoUtils {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoUtils() {}

    /** 生成订单号：yyyyMMddHHmmss + 4位自增序列 */
    public static String generateOrderNo() {
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return LocalDateTime.now().format(DATE_FMT) + String.format("%04d", seq);
    }

    /** genOrderNo 别名（兼容旧调用） */
    public static String genOrderNo() {
        return generateOrderNo();
    }

    /** 生成支付单号 */
    public static String genPayNo() {
        return "P" + generateOrderNo();
    }

    /** 生成退款单号 */
    public static String genRefundNo() {
        return "R" + generateOrderNo();
    }

    /** 生成拼团码：8位随机字母数字 */
    public static String generateGroupCode() {
        return RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 8);
    }
}
