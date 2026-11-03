package com.jaxon.smart_gym.identity.internal.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
实现统一的 401 响应
*/
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String traceId = TraceIdUtils.get(request);

        ApiResponse<Void> responseBody = ApiResponse.failure(
                "AUTH_TOKEN_INVALID",
                "未登录或登录已过期",
                traceId);

        jsonMapper.writeValue(response.getWriter(), responseBody);
    }

}
