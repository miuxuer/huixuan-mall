package com.huixuan.item.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.huixuan.api.dto.ItemDTO;
import com.huixuan.api.dto.OrderDetailDTO;
import com.huixuan.common.exception.BizIllegalException;
import com.huixuan.common.utils.BeanUtils;
import com.huixuan.item.domain.po.Item;
import com.huixuan.item.mapper.ItemMapper;
import com.huixuan.item.service.IItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author miuxuer
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    @Override
    @Transactional
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.huixuan.item.mapper.ItemMapper.updateStock";
        boolean r = false;
        try {
            r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足!", e);
        }
        if (!r) {
            throw new BizIllegalException("库存不足！");
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    /**
     * 批量恢复库存
     */
    @Override
    public void restoreStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.huixuan.item.mapper.ItemMapper.restoreStock";
        boolean r = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        if (!r) {
            throw new BizIllegalException("恢复库存失败！");
        }

    }
}
