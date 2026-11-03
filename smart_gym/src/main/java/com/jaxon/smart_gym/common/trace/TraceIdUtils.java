package com.jaxon.smart_gym.common.trace;

import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

public final class TraceIdUtils {

    public static final String HEADER_NAME = "X-Request-Id";

    public static final String MDC_KEY = "traceId";

    private static final String ATTRIBUTE_NAME = TraceIdUtils.class.getName() + ".TRACE_ID";

    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    /**
     * 从请求属性读取；尚未初始化时，校验请求头或生成新 ID。
     * 同一次请求始终复用同一个值。
     */
    public static String get(HttpServletRequest request) {
        Object attribute = request.getAttribute(ATTRIBUTE_NAME);

        if (attribute instanceof String traceId && !traceId.isBlank()) {
            return traceId;
        }

        String candidate = request.getHeader(HEADER_NAME);

        String traceId;
        if (candidate != null && VALID_TRACE_ID.matcher(candidate).matches()) {
            traceId = candidate;
        } else {
            traceId = generate();
        }

        request.setAttribute(ATTRIBUTE_NAME, traceId);

        return traceId;

    }

    private static String generate() {
        return "tr_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
