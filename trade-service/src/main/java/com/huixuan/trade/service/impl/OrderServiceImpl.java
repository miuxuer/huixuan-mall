package com.huixuan.trade.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.huixuan.api.client.CartClient;
import com.huixuan.api.client.ItemClient;
import com.huixuan.api.dto.ItemDTO;
import com.huixuan.api.dto.OrderDetailDTO;
import com.huixuan.common.exception.BadRequestException;
import com.huixuan.common.utils.UserContext;
import com.huixuan.trade.constants.MQConstants;
import com.huixuan.trade.domain.dto.OrderFormDTO;
import com.huixuan.trade.domain.po.Order;
import com.huixuan.trade.domain.po.OrderDetail;
import com.huixuan.trade.mapper.OrderMapper;
import com.huixuan.trade.service.IOrderDetailService;
import com.huixuan.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<com.huixuan.api.dto.OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        cartClient.removeByItemIds(itemIds);

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        // 发送延时消息，检查订单状态
        rabbitTemplate.convertAndSend(MQConstants.DELAY_EXCHANGE_NAME, MQConstants.DELAY_ORDER_KEY, order.getId(), message -> {
            message.getMessageProperties().setDelayLong(1000*60*15L); // 15分钟后自动取消订单
            return message;
        });
        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    /**
     * 取消订单，恢复库存
     */
    @Override
    public void cancelOrder(Long orderId) {
        // 修改订单状态
        boolean update = lambdaUpdate()
                .set(Order::getStatus, 5)
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 1)  // 只有未付款的时候才能取消订单
                .update();

        if(!update){
            // 订单已经被支付或者已经被取消，直接结束
            return;
        }
        // 根据订单id，查询到所有商品
        List<OrderDetail> details = detailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .list();

        List<OrderDetailDTO> detailDTOS = details.stream().map(p -> new OrderDetailDTO()
                        .setItemId(p.getItemId())
                        .setNum(p.getNum()))
                .toList();
        itemClient.restoreStock(detailDTOS);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
