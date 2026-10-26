package com.huixuan.pay.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 同上：@Builder 抑制了无参构造器，Jackson 3 需要它来反序列化请求体
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付确认表单实体")
public class PayOrderFormDTO {
    @Schema(description = "支付订单id不能为空")
    @NotNull(message = "支付订单id不能为空")
    private Long id;
    @Schema(description = "支付密码")
    @NotNull(message = "支付密码")
    private String pw;
}