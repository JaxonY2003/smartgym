package com.jaxon.smart_gym.identity.internal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;
import com.jaxon.smart_gym.identity.internal.dto.request.LoginRequest;
import com.jaxon.smart_gym.identity.internal.dto.request.RefreshTokenRequest;
import com.jaxon.smart_gym.identity.internal.dto.request.RegisterRequest;
import com.jaxon.smart_gym.identity.internal.dto.response.CurrentUserResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.LoginResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.RefreshTokenResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.RegisterResponse;
import com.jaxon.smart_gym.identity.internal.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        RegisterResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "注册成功",
                        data,
                        traceId(httpRequest)
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse data = authService.login(
                request,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(
                ApiResponse.success(
                        "登录成功",
                        data,
                        traceId(httpRequest)
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        RefreshTokenResponse data = authService.refresh(
                request,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(
                ApiResponse.success(
                        data,
                        traceId(httpRequest)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> currentUser(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        CurrentUserResponse data = authService.currentUser(userId(jwt));
        return ResponseEntity.ok(
                ApiResponse.success(
                        data,
                        traceId(httpRequest)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        authService.logout(userId(jwt));
        return ResponseEntity.ok(
                ApiResponse.success(traceId(httpRequest))
        );
    }

    private Long userId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "AUTH_TOKEN_INVALID",
                    "登录状态无效，请重新登录",
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdUtils.get(request);
    }
}
