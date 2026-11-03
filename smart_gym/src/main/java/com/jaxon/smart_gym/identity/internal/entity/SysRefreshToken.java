package com.jaxon.smart_gym.identity.internal.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.smart_gym.common.entity.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@TableName("sys_refresh_token")
@Getter
@Setter
public class SysRefreshToken extends BaseEntity{

    private Long userId;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private String userAgent;

    private String clientIp;

}
