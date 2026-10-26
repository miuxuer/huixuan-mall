package com.huixuan.trade.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档信息。这些内容原来是写在 application.yaml 的 knife4j.openapi 下面的，
 * 换成 springdoc 之后没有对应的配置项，改成用 OpenAPI Bean 描述。
 * 分组（只扫描 com.huixuan.controller）仍在 application.yaml 的 springdoc.packages-to-scan 里配。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI huixuanOpenApi() {
        return new OpenAPI().info(new Info()
                .title("miuxuer商城交易接口文档")
                .description("miuxuer商城交易接口文档")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("miuxuer")
                        .email("miuxuer@outlook.com")
                        .url("https://github.com/miuxuer")));
    }
}
