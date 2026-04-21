package com.shop.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付回调通知DTO")
public class PayNotifyDTO {

    @Schema(description = "微信支付通知ID")
    private String id;

    @Schema(description = "通知创建时间")
    private String createTime;

    @Schema(description = "通知类型")
    private String eventType;

    @Schema(description = "通知数据")
    private String resource;
}
