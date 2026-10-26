package com.huixuan.trade.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.huixuan.trade.domain.dto.OrderFormDTO;
import com.huixuan.trade.domain.po.Order;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO);

    void markOrderPaySuccess(Long orderId);

    void cancelOrder(Long orderId);
}
