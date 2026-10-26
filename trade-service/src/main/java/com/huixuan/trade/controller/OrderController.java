package com.huixuan.trade.controller;

import com.huixuan.common.utils.BeanUtils;
import com.huixuan.trade.domain.dto.OrderFormDTO;
import com.huixuan.trade.domain.vo.OrderVO;
import com.huixuan.trade.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理接口")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderService orderService;

    @Operation(summary = "根据id查询订单")
    @GetMapping("{id}")
    public OrderVO queryOrderById(@Parameter(description = "订单id") @PathVariable("id") Long orderId) {
        return BeanUtils.copyBean(orderService.getById(orderId), OrderVO.class);
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Long createOrder(@RequestBody OrderFormDTO orderFormDTO){
        return orderService.createOrder(orderFormDTO);
    }

    @Operation(summary = "标记订单已支付")
    @PutMapping("/{orderId}")
    public void markOrderPaySuccess(@Parameter(description = "订单id") @PathVariable("orderId") Long orderId) {
        orderService.markOrderPaySuccess(orderId);
    }
}
