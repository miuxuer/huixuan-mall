package com.huixuan.item.service;

import com.huixuan.common.domain.PageDTO;
import com.huixuan.item.domain.po.ItemDoc;
import com.huixuan.item.domain.query.ItemPageQuery;

import java.util.List;
import java.util.Map;

/**
 * 商品搜索服务（数据来自 Elasticsearch 的 items 索引）。
 */
public interface IItemSearchService {

    /**
     * 关键字 + 过滤条件 + 排序 + 分页搜索商品
     */
    PageDTO<ItemDoc> search(ItemPageQuery query);

    /**
     * 查询搜索页可选的过滤项：分类、品牌
     */
    Map<String, List<String>> filters(ItemPageQuery query);
}
