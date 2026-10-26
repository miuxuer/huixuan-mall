package com.huixuan.pay.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Builder 会抑制掉无参构造器，Jackson 3（Boot 4）就反序列化不了请求体，
// 所以必须显式补回 @NoArgsConstructor，@Builder 还需要全参构造器，一起加上
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付下单表单实体")
public class PayApplyDTO {
    @Schema(description = "业务订单id不能为空")
    @NotNull(message = "业务订单id不能为空")
    private Long bizOrderNo;
    @Schema(description = "支付金额必须为正数")
    @Min(value = 1, message = "支付金额必须为正数")
    private Integer amount;
    @Schema(description = "支付渠道编码不能为空")
    @NotNull(message = "支付渠道编码不能为空")
    private String payChannelCode;
    @Schema(description = "支付方式不能为空")
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
    @Schema(description = "订单中的商品信息不能为空")
    @NotNull(message = "订单中的商品信息不能为空")
    private String orderInfo;
}