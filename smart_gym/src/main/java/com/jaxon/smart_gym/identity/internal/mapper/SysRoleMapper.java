package com.jaxon.smart_gym.identity.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.identity.internal.entity.SysRole;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<String> selectRoleCodesByUserId(Long userId);
}
