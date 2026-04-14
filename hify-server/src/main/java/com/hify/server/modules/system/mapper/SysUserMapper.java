package com.hify.server.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.server.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 *
 * @author hify
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
