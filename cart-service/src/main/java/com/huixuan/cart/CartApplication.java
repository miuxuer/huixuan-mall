package com.huixuan.cart;

import com.huixuan.api.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.huixuan.api.client" , defaultConfiguration = DefaultFeignConfig.class)
@MapperScan("com.huixuan.cart.mapper")
@SpringBootApplication
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
/*
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }*/
}