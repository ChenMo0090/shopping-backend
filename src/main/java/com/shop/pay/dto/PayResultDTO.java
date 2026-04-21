package com.shop.pay.dto;

import lombok.Data;

@Data
public class PayResultDTO {
    /** 商户侧单号 */
    private String outTradeNo;
    /** 时间戳 */
    private String timeStamp;
    /** 随机串 */
    private String nonceStr;
    /** package 值（prepay_id） */
    private String packageVal;
    /** 签名算法 */
    private String signType;
    /** 签名 */
    private String paySign;
    /** 支付状态：0=待支付 1=已支付 2=已退款 */
    private Integer status;
}
