package com.huixuan.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.huixuan.user.domain.dto.LoginFormDTO;
import com.huixuan.user.domain.po.User;
import com.huixuan.user.domain.vo.UserLoginVO;


/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
public interface IUserService extends IService<User> {

    UserLoginVO login(LoginFormDTO loginFormDTO);

    void deductMoney(String pw, Integer totalFee);
}
