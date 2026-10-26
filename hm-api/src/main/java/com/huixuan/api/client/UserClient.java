package com.huixuan.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("user-service")
public interface UserClient {
    @PutMapping("/users/money/deduct")
    // 注意：user-service 的接口参数名是 amount，不是 totalFee，两边必须一致
    void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);
}
