package com.huixuan.item.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.huixuan.api.dto.ItemDTO;
import com.huixuan.api.dto.OrderDetailDTO;
import com.huixuan.item.domain.po.Item;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
public interface IItemService extends IService<Item> {

    void deductStock(List<OrderDetailDTO> items);

    List<ItemDTO> queryItemByIds(Collection<Long> ids);

    void restoreStock(List<OrderDetailDTO> items);
}
