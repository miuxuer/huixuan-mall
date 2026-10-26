package com.huixuan.pay.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.huixuan.pay.domain.dto.PayApplyDTO;
import com.huixuan.pay.domain.dto.PayOrderFormDTO;
import com.huixuan.pay.domain.po.PayOrder;

/**
 * <p>
 * 支付订单 服务类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-16
 */
public interface IPayOrderService extends IService<PayOrder> {

    String applyPayOrder(PayApplyDTO applyDTO);

    void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO);
}
