package com.jaxon.smart_gym.identity.internal.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@TableName("sys_user_role")
@Getter
@Setter
public class SysUserRole extends BaseEntity{
    
    private Long userId;

    private Long roleId;

}
