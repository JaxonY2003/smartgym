package com.jaxon.smart_gym.identity.internal.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@TableName("sys_user")
@Getter 
@Setter 
public class SysUser extends BaseEntity{

    private String account;

    private String passwordHash;

    private String displayName;

    private String avatarUrl;

    private String status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime deletedAt;

    
}
