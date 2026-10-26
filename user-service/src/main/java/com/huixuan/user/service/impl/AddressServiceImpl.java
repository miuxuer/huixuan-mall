package com.huixuan.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.huixuan.user.domain.po.Address;
import com.huixuan.user.mapper.AddressMapper;
import com.huixuan.user.service.IAddressService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author miuxuer
 * @since 2023-05-05
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

}
