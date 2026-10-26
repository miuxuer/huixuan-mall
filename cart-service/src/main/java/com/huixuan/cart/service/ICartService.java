package com.huixuan.cart.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.huixuan.cart.domain.dto.CartFormDTO;
import com.huixuan.cart.domain.po.Cart;
import com.huixuan.cart.domain.vo.CartVO;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 订单详情表 服务类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
public interface ICartService extends IService<Cart> {

    void addItem2Cart(CartFormDTO cartFormDTO);

    List<CartVO> queryMyCarts();

    void removeByItemIds(Collection<Long> itemIds);
}
