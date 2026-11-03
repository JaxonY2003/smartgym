package com.jaxon.smart_gym.identity.internal.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;
import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
实现统一的 403 响应
*/

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String traceId = TraceIdUtils.get(request);

        ApiResponse<Void> responseBody = ApiResponse.failure(
                "FORBIDDEN",
                "没有权限执行此操作",
                traceId);

        jsonMapper.writeValue(response.getWriter(), responseBody);
    }
}
