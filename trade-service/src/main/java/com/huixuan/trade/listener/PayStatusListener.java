package com.huixuan.trade.listener;

import com.huixuan.trade.domain.po.Order;
import com.huixuan.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusListener {

    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    public void listenerPaySuccess(Long orderId) {
        // 根据id查询订单
        Order order = orderService.getById(orderId);
        // 判断订单是否不是未支付状态
        if (order == null || order.getStatus() != 1) {
            // 不是未支付状态，不处理
            return;
        }
        orderService.markOrderPaySuccess(orderId);
    }
}
