package com.jaxon.smart_gym.common.trace;

import java.io.IOException;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = TraceIdUtils.get(request);

        response.setHeader(TraceIdUtils.HEADER_NAME, traceId);

        String previousTraceId = MDC.get(TraceIdUtils.MDC_KEY);

        boolean logRequest = request.getRequestURI().startsWith("/api/");
        long startedAt = System.nanoTime();
        boolean completed = false;
        try {
            MDC.put(TraceIdUtils.MDC_KEY, traceId);
            if (logRequest) {
                log.info("http_request_started method={} path={}", request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            if (logRequest) {
                long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
                if (completed) {
                    log.info("http_request_completed method={} path={} status={} elapsedMs={} asyncStarted={}",
                            request.getMethod(), request.getRequestURI(), response.getStatus(),
                            elapsedMs, request.isAsyncStarted());
                } else {
                    log.warn("http_request_failed method={} path={} elapsedMs={}",
                            request.getMethod(), request.getRequestURI(), elapsedMs);
                }
            }
            if (previousTraceId == null) {
                MDC.remove(TraceIdUtils.MDC_KEY);
            } else {
                MDC.put(TraceIdUtils.MDC_KEY, previousTraceId);
            }
        }
    }

}
