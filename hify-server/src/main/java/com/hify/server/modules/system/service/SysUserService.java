package com.hify.server.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hify.server.modules.system.entity.SysUser;

/**
 * 系统用户 Service
 *
 * @author hify
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);
}
