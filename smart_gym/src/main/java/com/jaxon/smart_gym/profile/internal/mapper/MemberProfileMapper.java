package com.jaxon.smart_gym.profile.internal.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.smart_gym.profile.internal.entity.MemberProfile;

@Mapper
public interface MemberProfileMapper extends BaseMapper<MemberProfile> {
}
