package com.huixuan.item.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

/**
 * Elasticsearch 客户端配置。
 * <p>
 * 项目用的是 co.elastic.clients:elasticsearch-java 9.x，底层 REST 客户端是 HC5 的 Rest5Client，链路为：
 * <pre>
 *   Rest5Client → Rest5ClientTransport → ElasticsearchClient
 * </pre>
 * ES 地址从 {@code hm.es.addr} 读取，dev 环境走 docker 网络里的 es 容器，local 环境覆盖成虚拟机 IP。
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${hm.es.addr:http://es:9200}")
    private String esAddr;

    @Bean(destroyMethod = "")
    public Rest5Client rest5Client() {
        try {
            return Rest5Client.builder(HttpHost.create(esAddr)).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Elasticsearch 地址不合法: " + esAddr, e);
        }
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchTransport elasticsearchTransport(Rest5Client rest5Client) {
        // ItemDoc 里有 LocalDateTime，默认 ObjectMapper 不认识 Java 8 时间类型，
        // 必须注册 JavaTimeModule，否则反序列化 updateTime 会报错
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Rest5ClientTransport(rest5Client, new JacksonJsonpMapper(objectMapper));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
