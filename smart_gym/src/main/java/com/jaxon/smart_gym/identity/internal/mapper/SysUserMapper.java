package com.jaxon.smart_gym.identity.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.identity.internal.entity.SysUser;

@Mapper 
public interface SysUserMapper extends BaseMapper<SysUser>{
    
}
