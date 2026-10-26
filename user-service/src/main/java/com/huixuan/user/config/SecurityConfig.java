package com.huixuan.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * 读取 hmall.jks 里的 RSA 密钥对。
     * <p>
     * 原来用的是 spring-security-rsa 的 KeyStoreKeyFactory，但那个依赖最新只到 1.1.x，
     * Spring Boot 4 的 BOM 已经不再管理它（原来钉的 1.0.9.RELEASE 是 2019 年的版本）。
     * 这里要做的只是「从 JKS 里取一对密钥」，JDK 自带的 KeyStore 就够了，所以直接去掉了这个依赖。
     */
    @Bean
    public KeyPair keyPair(JwtProperties properties) throws Exception {
        char[] password = properties.getPassword().toCharArray();
        String alias = properties.getAlias();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = properties.getLocation().getInputStream()) {
            keyStore.load(in, password);
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
        Certificate certificate = keyStore.getCertificate(alias);
        if (privateKey == null || certificate == null) {
            throw new IllegalStateException("JKS 中找不到别名 " + alias + " 对应的密钥对");
        }
        PublicKey publicKey = certificate.getPublicKey();
        return new KeyPair(publicKey, privateKey);
    }
}
