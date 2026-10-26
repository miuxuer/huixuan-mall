package com.huixuan.item.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.huixuan.common.utils.BeanUtils;
import com.huixuan.item.domain.po.Item;
import com.huixuan.item.domain.po.ItemDoc;
import com.huixuan.item.service.IItemService;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档操作测试：把 MySQL 里的商品批量导入 ES。
 * <p>
 * 用 {@code @SpringBootTest} 是为了拿 {@link IItemService} 查库，
 * 所以必须激活 local 环境（连 192.168.100.128 的 MySQL / Nacos）。
 * <p>
 * 注意：项目用的是新版客户端 elasticsearch-java 9.x，
 * 批量写入的 API 是 {@code client.bulk(b -> b.operations(ops))}，
 * 不是旧版客户端的 {@code client.bulk(new BulkRequest(), RequestOptions.DEFAULT)}。
 */
@SpringBootTest
@ActiveProfiles("local")
class ElasticDocumentTest {

    private static final String ES_URL = "http://192.168.100.128:9200";
    private static final String INDEX_NAME = "items";

    @Autowired
    private IItemService itemService;

    private Rest5Client restClient;
    private ElasticsearchTransport transport;
    private ElasticsearchClient client;

    @BeforeEach
    void setUp() throws URISyntaxException {
        // ItemDoc 里有 LocalDateTime，默认的 ObjectMapper 不认识 Java 8 时间类型，
        // 必须注册 JavaTimeModule，否则序列化时报
        // "Java 8 date/time type LocalDateTime not supported by default"
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        restClient = Rest5Client.builder(HttpHost.create(ES_URL)).build();
        transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper(mapper));
        client = new ElasticsearchClient(transport);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (transport != null) {
            transport.close();
        }
    }

    /**
     * 分页从 MySQL 读商品，批量写入 ES。
     * <p>
     * 每页 500 条，读完为止。一页一页地灌，避免一次性把 8 万多条商品全读进内存。
     */
    @Test
    void testBulkDoc() throws IOException {
        // 索引必须先建好：否则 _bulk 会自动创建一个“动态映射”的索引，
        // name 会被切成标准分词器，后面搜索的效果就不对了。
        if (!client.indices().exists(e -> e.index(INDEX_NAME)).value()) {
            throw new IllegalStateException(
                    "索引 " + INDEX_NAME + " 不存在，请先执行 ElasticIndexTest 里的建索引方法（或手动 curl 创建）");
        }

        int pageNo = 1, pageSize = 500;
        long total = 0;
        while (true) {
            // 1. 准备文档数据：只导上架商品（status=1）
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();

            // 没有更多数据了
            if (records == null || records.isEmpty()) {
                System.out.println("导入完成，共 " + total + " 条");
                return;
            }

            // 2. 准备请求参数：每条商品转成一个 bulk 的 index 操作
            List<BulkOperation> ops = new ArrayList<>(records.size());
            for (Item item : records) {
                ItemDoc doc = BeanUtils.copyBean(item, ItemDoc.class);
                // Item.id 是 Long，ItemDoc.id 是 String，hutool 一般能自动转，这里再保险一下
                doc.setId(String.valueOf(item.getId()));

                ops.add(BulkOperation.of(o -> o.index(i -> i
                        .index(INDEX_NAME)
                        .id(String.valueOf(item.getId()))
                        .document(doc))));
            }

            // 3. 发送请求
            BulkResponse response = client.bulk(b -> b.operations(ops));

            // 4. 检查结果：bulk 不会整体失败，要逐条看
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    System.err.printf("写入失败 id=%s, reason=%s%n",
                            item.id(), item.error().reason());
                }
            }

            total += records.size();
            System.out.printf("第 %d 页导入完成，累计 %d 条%n", pageNo, total);

            // 5. 翻页
            pageNo++;
        }
    }
}
