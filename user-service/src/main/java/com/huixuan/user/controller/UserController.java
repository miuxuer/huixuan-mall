package com.huixuan.user.controller;

import com.huixuan.user.domain.dto.LoginFormDTO;
import com.huixuan.user.domain.vo.UserLoginVO;
import com.huixuan.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "用户登录接口")
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO){
        return userService.login(loginFormDTO);
    }

    @Operation(summary = "扣减余额")
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam("pw") @Parameter(description = "支付密码") String pw,
                            @RequestParam("amount") @Parameter(description = "支付金额") Integer amount){
        userService.deductMoney(pw, amount);
    }
}

