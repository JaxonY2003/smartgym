package com.jaxon.smart_gym.identity.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 128, message = "账号长度必须为 3～128 个字符")
        String account,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为 8～64 个字符")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 30, message = "昵称长度必须为 2～30 个字符")
        String displayName
) {
    public RegisterRequest {
        account = account == null ? null : account.trim().toLowerCase(Locale.ROOT);
        // 昵称只去除首尾空白；密码保持原样，避免改变用户输入的口令。
        displayName = displayName == null ? null : displayName.trim();
    }
}
