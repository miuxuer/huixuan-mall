package com.huixuan.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;

/**
 * Spring Boot 4 默认用 Jackson 3，包名由 com.fasterxml.jackson 改成 tools.jackson。
 * 原来的 Jackson2ObjectMapperBuilderCustomizer 在 Boot 4 已经没有了，这里改成暴露一个
 * JacksonModule Bean —— Boot 4 的 JacksonAutoConfiguration 会自动收集容器里所有
 * JacksonModule 并 addModules 到 JsonMapper 上。
 */
@Configuration
@ConditionalOnClass(JsonMapper.class)
public class JsonConfig {

    @Bean
    public JacksonModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        // long -> string，避免前端 JS 精度丢失
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        return module;
    }
}
