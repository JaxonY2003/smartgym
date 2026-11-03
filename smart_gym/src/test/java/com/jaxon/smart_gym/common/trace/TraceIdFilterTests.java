package com.jaxon.smart_gym.common.trace;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class TraceIdFilterTests {

    @Test
    void logsStartAndCompletionWithTraceStatusAndDurationAndRestoresMdc() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(TraceIdFilter.class);
        var appender = capture(logger);
        MDC.put(TraceIdUtils.MDC_KEY, "previous");
        try {
            var request = new MockHttpServletRequest("PATCH", "/api/v1/workout-plans/3");
            request.addHeader(TraceIdUtils.HEADER_NAME, "patch-investigation-1");
            request.setQueryString("secret=must-not-be-logged");
            var response = new MockHttpServletResponse();
            new TraceIdFilter().doFilter(request, response, (req, res) -> {
                assertEquals("patch-investigation-1", MDC.get(TraceIdUtils.MDC_KEY));
                response.setStatus(200);
            });
            assertEquals("patch-investigation-1", response.getHeader(TraceIdUtils.HEADER_NAME));
            assertEquals("previous", MDC.get(TraceIdUtils.MDC_KEY));
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("http_request_started"));
            String completed = appender.list.get(1).getFormattedMessage();
            assertTrue(completed.contains("status=200"));
            assertTrue(completed.contains("elapsedMs="));
            assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("secret=")));
            assertEquals("patch-investigation-1",
                    appender.list.get(1).getMDCPropertyMap().get(TraceIdUtils.MDC_KEY));
        } finally {
            MDC.remove(TraceIdUtils.MDC_KEY);
            logger.detachAppender(appender);
        }
    }

    @Test
    void escapingExceptionLogsFailureAndPreservesException() {
        Logger logger = (Logger) LoggerFactory.getLogger(TraceIdFilter.class);
        var appender = capture(logger);
        try {
            var request = new MockHttpServletRequest("PATCH", "/api/v1/workout-plans/3");
            var response = new MockHttpServletResponse();
            var expected = new IOException("connection closed");
            IOException actual = assertThrows(IOException.class, () -> new TraceIdFilter().doFilter(
                    request, response, (req, res) -> { throw expected; }));
            assertSame(expected, actual);
            assertTrue(appender.list.get(1).getFormattedMessage().contains("http_request_failed"));
            assertNull(MDC.get(TraceIdUtils.MDC_KEY));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> capture(Logger logger) {
        var appender = new ListAppender<ILoggingEvent>() {
            @Override
            protected void append(ILoggingEvent event) {
                // Snapshot MDC while the request's trace is still in scope.
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
