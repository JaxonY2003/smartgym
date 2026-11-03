package com.jaxon.smart_gym.identity.internal.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@TableName("sys_role")
@Getter
@Setter
public class SysRole extends BaseEntity{

    private String code;

    private String name;

    private String description;
    
}
