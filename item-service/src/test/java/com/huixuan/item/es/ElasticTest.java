package com.huixuan.item.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elasticsearch 连接测试。
 * <p>
 * 注意版本差异：项目用的是 co.elastic.clients:elasticsearch-java 9.x，
 * 底层 REST 客户端已经从 HC4 的 RestClient 换成了 HC5 的 Rest5Client。
 * 所以是：
 * <pre>
 *   Rest5Client → Rest5ClientTransport → ElasticsearchClient
 * </pre>
 * 而不是旧版客户端的 RestClientTransport + org.elasticsearch.client.RestClient。
 * <p>
 * 这里不启动 Spring 容器，直接手动建客户端 —— 跑得快，也不依赖 Nacos / MySQL / Seata。
 */
class ElasticTest {

    /**
     * ES 地址，和 docker 里映射的 9200 对应
     */
    private static final String ES_URL = "http://192.168.100.128:9200";

    /**
     * 测试专用索引，测完删掉，别污染业务索引
     */
    private static final String TEST_INDEX = "test_items";

    // private static final String INDEX_NAME = "items";

    private Rest5Client restClient;
    private ElasticsearchTransport transport;
    private ElasticsearchClient client;

    @BeforeEach
    void setUp() throws URISyntaxException {
        // 1. 底层 HTTP 客户端：指定 ES 节点
        restClient = Rest5Client.builder(HttpHost.create(ES_URL)).build();

        // 2. 传输层：负责把 Java 对象序列化成 JSON（Jackson）
        transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());

        // 3. 业务客户端：后面所有 index/search/indices 操作都走它
        client = new ElasticsearchClient(transport);

        System.out.println("ElasticsearchClient 初始化完成 -> " + ES_URL);
    }

    @AfterEach
    void tearDown() throws IOException {
        // 关闭连接池，否则测试多了会有一堆空闲连接
        if (transport != null) {
            transport.close();
        }
    }

    /**
     * 测试 1：能不能连上，并打印集群信息。
     * 这是最基础的连接测试。
     */
    @Test
    void testConnection() throws IOException {
        InfoResponse info = client.info();

        System.out.println("集群名称 : " + info.clusterName());
        System.out.println("集群 UUID: " + info.clusterUuid());
        System.out.println("节点名称 : " + info.name());
        System.out.println("ES 版本  : " + info.version().number());
        System.out.println("Lucene   : " + info.version().luceneVersion());

        // 能拿到版本号就说明连接、序列化、反序列化整条链路都通了
        assertNotNull(info.version().number());
        assertFalse(info.version().number().isBlank());
    }

    /**
     * 测试 2：ping。
     * 只关心“活着没”，不解析响应体，比 info() 更轻。
     */
    @Test
    void testPing() throws IOException {
        boolean alive = client.ping().value();
        System.out.println("ES 存活: " + alive);
        assertTrue(alive, "连不上 ES，检查 9200 端口和 xpack.security 配置");
    }

    /**
     * 测试 3：一次完整的读写链路。
     * 建索引 -> 写文档 -> 查文档 -> 搜索 -> 删索引。
     * 验证的不只是“连得上”，而是“能用”。
     */
    @Test
    void testIndexAndSearch() throws IOException {
        // 0. 如果上次测试残留了，先删掉
        if (client.indices().exists(e -> e.index(TEST_INDEX)).value()) {
            client.indices().delete(d -> d.index(TEST_INDEX));
        }

        // 1. 创建索引
        client.indices().create(c -> c.index(TEST_INDEX));
        System.out.println("索引创建成功: " + TEST_INDEX);

        // 2. 写入一条文档（ES 9 里 type 概念已经彻底移除，只有 index + id）
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", 1);
        doc.put("name", "小米手机");
        doc.put("price", 349900);
        doc.put("stock", 100);

        // refresh(WAIT_FOR)：等索引刷新完再返回。
        // ES 是近实时的，不加这个，紧接着的 search 可能搜不到刚写的文档。
        client.index(i -> i.index(TEST_INDEX).id("1").document(doc).refresh(Refresh.WaitFor));
        System.out.println("文档写入成功: id=1");

        // 3. 按 id 查回来
        GetResponse<Map> got = client.get(g -> g.index(TEST_INDEX).id("1"), Map.class);
        assertTrue(got.found());
        System.out.println("按 id 查到: " + got.source());

        // 4. 搜索（这里用 matchAll，实际业务会用 match / term / 分页）
        SearchResponse<Map> search = client.search(
                s -> s.index(TEST_INDEX).query(q -> q.matchAll(m -> m)),
                Map.class
        );
        System.out.println("搜索结果总数: " + search.hits().total().value());
        assertEquals(1, search.hits().total().value());

        // 5. 清理
        client.indices().delete(d -> d.index(TEST_INDEX));
        System.out.println("索引已删除: " + TEST_INDEX);

    }


    @Test
    void createIndex() throws IOException {
        // 先删后建，保证多次跑测试幂等
        BooleanResponse exists = client.indices().exists(e -> e.index(TEST_INDEX));
        if (exists.value()) {
            client.indices().delete(d -> d.index(TEST_INDEX));
        }

        CreateIndexResponse resp = client.indices().create(c -> c
                .index(TEST_INDEX)                               // ← 索引名
                .withJson(new StringReader(MAPPING_TEMPLATE))    // ← mapping JSON
        );
        System.out.println("acknowledged = " + resp.acknowledged() + ", index = " + resp.index());
    }


    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
