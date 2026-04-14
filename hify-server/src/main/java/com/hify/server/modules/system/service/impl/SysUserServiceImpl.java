package com.hify.server.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.server.modules.system.entity.SysUser;
import com.hify.server.modules.system.mapper.SysUserMapper;
import com.hify.server.modules.system.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 系统用户 Service 实现
 *
 * @author hify
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return getOne(wrapper);
    }
}
