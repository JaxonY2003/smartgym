package com.jaxon.smart_gym.identity.internal.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginRequest(
        @NotBlank(message = "账号不能为空")
        String account,

        @NotBlank(message = "密码不能为空")
        String password
) {
    public LoginRequest {
        account = account == null ? null : account.trim().toLowerCase(Locale.ROOT);
        // 密码不 trim，登录必须匹配注册时保存的原始口令。
    }
}
