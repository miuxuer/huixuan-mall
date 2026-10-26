package com.huixuan.api.config;

import com.huixuan.api.client.fallback.ItemClientFallbackFactory;
import com.huixuan.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    /**
     * 日志配置
     */
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

    /**
     * 向请求头中添加用户信息
     */
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userInfo = UserContext.getUser();
                if(userInfo != null) {
                    template.header("user-info", userInfo.toString());
                }
            }
        };
    }

    /**
     *注册服务降级
     */
    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory(){
        return new ItemClientFallbackFactory();
    }
}
