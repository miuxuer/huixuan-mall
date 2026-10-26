package com.huixuan.trade.listener;

import com.huixuan.api.client.PayClient;
import com.huixuan.api.dto.PayOrderDTO;
import com.huixuan.trade.constants.MQConstants;
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
public class OrderDelayMessageListener {

    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MQConstants.DELAY_ORDER_QUEUE_NAME),
            exchange = @Exchange(value = MQConstants.DELAY_EXCHANGE_NAME, delayed = "true"),
            key = {MQConstants.DELAY_ORDER_KEY}
    ))
    public void listenerOrderDelayMessage(Long orderId) {
        // 查询订单
        Order order = orderService.getById(orderId);
        // 查看订单状态
        if (order == null || order.getStatus() != 1) {
            return;
        }
        // 未支付，查看支付流水状态
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);
        // 判断是否支付
        if (payOrder != null && payOrder.getStatus() == 3) {
            // 已支付，更新订单状态
            orderService.markOrderPaySuccess(orderId);
        } else {
            // 未支付，取消订单，恢复库存
            orderService.cancelOrder(orderId);
        }


    }
}
