package com.huixuan.trade.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.huixuan.trade.domain.po.OrderDetail;
import com.huixuan.trade.mapper.OrderDetailMapper;
import com.huixuan.trade.service.IOrderDetailService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单详情表 服务实现类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements IOrderDetailService {

}
