package com.huixuan.item.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.huixuan.common.domain.PageDTO;
import com.huixuan.common.domain.PageQuery;
import com.huixuan.common.exception.BizIllegalException;
import com.huixuan.item.domain.po.ItemDoc;
import com.huixuan.item.domain.query.ItemPageQuery;
import com.huixuan.item.service.IItemSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 商品搜索服务实现，查询 Elasticsearch 的 items 索引。
 */
@Service
@RequiredArgsConstructor
public class ItemSearchServiceImpl implements IItemSearchService {

    /**
     * 商品索引名，商品数据已经提前灌好
     */
    private static final String INDEX_NAME = "items";
    /**
     * 聚合过滤项的名称
     */
    private static final String CATEGORY_AGG = "category";
    private static final String BRAND_AGG = "brand";
    /**
     * 过滤项最多返回多少个
     */
    private static final int AGG_SIZE = 100;
    /**
     * 允许排序的字段，其它字段（比如 name 是 text 类型）排序会报错
     */
    private static final List<String> SORTABLE_FIELDS = List.of("price", "sold");
    /**
     * 单页最大条数，防止前端传个超大值把 ES 拖垮
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final ElasticsearchClient client;

    @Override
    public PageDTO<ItemDoc> search(ItemPageQuery query) {
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1
                ? PageQuery.DEFAULT_PAGE_NUM : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? PageQuery.DEFAULT_PAGE_SIZE : query.getPageSize();
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        List<SortOptions> sorts = buildSort(query);
        int from = (pageNo - 1) * pageSize;
        int size = pageSize;
        BoolQuery boolQuery = buildBoolQuery(query.getKey(), query.getBrand(), query.getCategory(),
                query.getMinPrice(), query.getMaxPrice());

        try {
            SearchResponse<ItemDoc> response = client.search(s -> {
                s.index(INDEX_NAME)
                        .from(from)
                        .size(size)
                        // 默认 total 只精确到 10000，这里要拿准确总条数做分页
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.bool(boolQuery));
                if (!sorts.isEmpty()) {
                    s.sort(sorts);
                }
                return s;
            }, ItemDoc.class);

            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            List<ItemDoc> list = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            long pages = (total + pageSize - 1) / pageSize;
            return new PageDTO<>(total, pages, list);
        } catch (IOException e) {
            throw new BizIllegalException("商品搜索失败", e);
        }
    }

    @Override
    public Map<String, List<String>> filters(ItemPageQuery query) {
        // 过滤项只受关键字和价格影响：已经选中的品牌/分类不能再参与过滤，
        // 否则过滤项里就只剩自己一个选项了
        BoolQuery boolQuery = buildBoolQuery(query.getKey(), null, null,
                query.getMinPrice(), query.getMaxPrice());
        try {
            SearchResponse<Void> response = client.search(s -> s
                            .index(INDEX_NAME)
                            .size(0)
                            .query(q -> q.bool(boolQuery))
                            .aggregations(CATEGORY_AGG, a -> a.terms(t -> t.field(CATEGORY_AGG).size(AGG_SIZE)))
                            .aggregations(BRAND_AGG, a -> a.terms(t -> t.field(BRAND_AGG).size(AGG_SIZE))),
                    Void.class);

            Map<String, List<String>> result = new LinkedHashMap<>(2);
            result.put(CATEGORY_AGG, readBuckets(response, CATEGORY_AGG));
            result.put(BRAND_AGG, readBuckets(response, BRAND_AGG));
            return result;
        } catch (IOException e) {
            throw new BizIllegalException("查询商品过滤项失败", e);
        }
    }

    /**
     * 组装查询条件：关键字走分词匹配，品牌/分类走精确匹配，价格走范围过滤
     */
    private BoolQuery buildBoolQuery(String key, String brand, String category, Integer minPrice, Integer maxPrice) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (StrUtil.isNotBlank(key)) {
            // items 索引的 name 是 text + ik_max_word，用 match 才能走分词
            bool.must(m -> m.match(mt -> mt.field("name").query(key)));
        }
        if (StrUtil.isNotBlank(brand)) {
            bool.filter(f -> f.term(t -> t.field("brand").value(brand)));
        }
        if (StrUtil.isNotBlank(category)) {
            bool.filter(f -> f.term(t -> t.field("category").value(category)));
        }
        if (minPrice != null || maxPrice != null) {
            bool.filter(f -> f.range(r -> r.untyped(u -> {
                u.field("price");
                if (minPrice != null) {
                    u.gte(JsonData.of(minPrice));
                }
                if (maxPrice != null) {
                    u.lte(JsonData.of(maxPrice));
                }
                return u;
            })));
        }
        return bool.build();
    }

    /**
     * 排序：不传 sortBy 时按 ES 的相关度 _score 排序；传了就看前端要升序还是降序
     */
    private List<SortOptions> buildSort(ItemPageQuery query) {
        if (StrUtil.isBlank(query.getSortBy()) || !SORTABLE_FIELDS.contains(query.getSortBy())) {
            return List.of();
        }
        SortOrder order = Boolean.FALSE.equals(query.getIsAsc()) ? SortOrder.Desc : SortOrder.Asc;
        return List.of(SortOptions.of(so -> so.field(f -> f.field(query.getSortBy()).order(order))));
    }

    /**
     * 从聚合结果里取出桶的 key 列表
     */
    private List<String> readBuckets(SearchResponse<?> response, String aggName) {
        Aggregate aggregate = response.aggregations().get(aggName);
        if (aggregate == null || !aggregate.isSterms()) {
            return List.of();
        }
        return aggregate.sterms().buckets().array().stream()
                .map(StringTermsBucket::key)
                .map(FieldValue::stringValue)
                .filter(Objects::nonNull)
                .toList();
    }
}
