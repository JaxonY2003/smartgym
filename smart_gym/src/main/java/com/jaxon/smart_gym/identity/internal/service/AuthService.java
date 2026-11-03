package com.jaxon.smart_gym.identity.internal.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jaxon.smart_gym.common.exception.BusinessException;
import com.jaxon.smart_gym.identity.internal.dto.request.LoginRequest;
import com.jaxon.smart_gym.identity.internal.dto.request.RefreshTokenRequest;
import com.jaxon.smart_gym.identity.internal.dto.request.RegisterRequest;
import com.jaxon.smart_gym.identity.internal.dto.response.CurrentUserResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.LoginResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.LoginUserResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.RefreshTokenResponse;
import com.jaxon.smart_gym.identity.internal.dto.response.RegisterResponse;
import com.jaxon.smart_gym.identity.internal.entity.SysRefreshToken;
import com.jaxon.smart_gym.identity.internal.entity.SysRole;
import com.jaxon.smart_gym.identity.internal.entity.SysUser;
import com.jaxon.smart_gym.identity.internal.entity.SysUserRole;
import com.jaxon.smart_gym.identity.internal.mapper.SysRoleMapper;
import com.jaxon.smart_gym.identity.internal.mapper.SysUserMapper;
import com.jaxon.smart_gym.identity.internal.mapper.SysUserRoleMapper;
import com.jaxon.smart_gym.identity.internal.security.JwtTokenService;

@Service
public class AuthService {

    private static final String ACTIVE = "ACTIVE";
    private static final String MEMBER_ROLE = "MEMBER";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    public AuthService(
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            Clock clock) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String account = normalizeAccount(request.account());

        SysUser existingUser = findByAccount(account);
        if (existingUser != null) {
            throw new BusinessException(
                    "ACCOUNT_ALREADY_EXISTS",
                    "账号已存在",
                    HttpStatus.CONFLICT
            );
        }

        SysRole memberRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getCode, MEMBER_ROLE)
        );
        if (memberRole == null) {
            throw new BusinessException(
                    "DEFAULT_ROLE_NOT_FOUND",
                    "系统默认会员角色不存在",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        SysUser user = new SysUser();
        user.setAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setStatus(ACTIVE);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 处理并发注册时绕过预查询、直接命中账号唯一索引的情况。
            throw new BusinessException(
                    "ACCOUNT_ALREADY_EXISTS",
                    "账号已存在",
                    HttpStatus.CONFLICT
            );
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(memberRole.getId());
        userRoleMapper.insert(userRole);

        return new RegisterResponse(
                user.getId(),
                user.getAccount(),
                user.getDisplayName()
        );
    }

    @Transactional
    public LoginResponse login(
            LoginRequest request,
            String userAgent,
            String clientIp) {
        String account = normalizeAccount(request.account());
        SysUser user = findByAccount(account);

        if (user == null) {
            throw invalidCredentials();
        }

        if (!ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    "ACCOUNT_DISABLED",
                    "账号已被禁用",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        List<String> roles = findRoleCodes(user.getId());
        String accessToken = jwtTokenService.createAccessToken(user.getId(), roles);
        String refreshToken = refreshTokenService.issue(user.getId(), userAgent, clientIp);

        user.setLastLoginAt(LocalDateTime.now(clock));
        userMapper.updateById(user);

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenTtlSeconds(),
                toLoginUser(user, roles)
        );
    }

    @Transactional
    public RefreshTokenResponse refresh(
            RefreshTokenRequest request,
            String userAgent,
            String clientIp) {
        SysRefreshToken currentToken = refreshTokenService.findByRawToken(
                request.refreshToken()
        );
        if (!refreshTokenService.isUsable(currentToken, LocalDateTime.now(clock))) {
            throw invalidRefreshToken();
        }

        SysUser user = findById(currentToken.getUserId());
        if (user == null) {
            throw invalidRefreshToken();
        }
        if (!ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    "ACCOUNT_DISABLED",
                    "账号已被禁用",
                    HttpStatus.FORBIDDEN
            );
        }

        List<String> roles = findRoleCodes(user.getId());
        String rotatedRefreshToken = refreshTokenService.rotate(
                currentToken,
                userAgent,
                clientIp
        );
        if (rotatedRefreshToken == null) {
            throw invalidRefreshToken();
        }

        String accessToken = jwtTokenService.createAccessToken(user.getId(), roles);
        return new RefreshTokenResponse(
                accessToken,
                rotatedRefreshToken,
                jwtTokenService.getAccessTokenTtlSeconds()
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Long userId) {
        SysUser user = findById(userId);
        if (user == null) {
            throw new BusinessException(
                    "AUTH_TOKEN_INVALID",
                    "当前用户不存在或登录状态已失效",
                    HttpStatus.UNAUTHORIZED
            );
        }
        if (!ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(
                    "ACCOUNT_DISABLED",
                    "账号已被禁用",
                    HttpStatus.FORBIDDEN
            );
        }

        return new CurrentUserResponse(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                findRoleCodes(user.getId()),
                toUtcOffset(user.getCreatedAt())
        );
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAll(userId);
    }

    private SysUser findByAccount(String account) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getAccount, account)
                        .isNull(SysUser::getDeletedAt)
        );
    }

    private SysUser findById(Long userId) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .isNull(SysUser::getDeletedAt)
        );
    }

    private List<String> findRoleCodes(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId)
                .stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private LoginUserResponse toLoginUser(SysUser user, List<String> roles) {
        return new LoginUserResponse(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                roles
        );
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(
                "AUTH_INVALID_CREDENTIALS",
                "账号或密码错误",
                HttpStatus.UNAUTHORIZED
        );
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(
                "AUTH_TOKEN_INVALID",
                "刷新令牌无效或已过期",
                HttpStatus.UNAUTHORIZED
        );
    }

    private OffsetDateTime toUtcOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase(Locale.ROOT);
    }
}
