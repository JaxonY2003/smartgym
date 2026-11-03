package com.jaxon.smart_gym.common.exception;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jaxon.smart_gym.common.response.ApiResponse;
import com.jaxon.smart_gym.common.trace.TraceIdUtils;

/**
 * REST 接口统一异常转换。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<?>> handleBusinessException(
                        BusinessException exception,
                        HttpServletRequest request) {
                return response(
                                exception.getStatus(),
                                ApiResponse.failure(
                                                exception.getCode(),
                                                exception.getMessage(),
                                                TraceIdUtils.get(request)));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                List<Map<String, String>> fieldErrors = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> Map.of(
                                                "field", error.getField(),
                                                "reason", error.getDefaultMessage() == null
                                                                ? "参数格式不正确"
                                                                : error.getDefaultMessage()))
                                .toList();

                return response(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                ApiResponse.failure(
                                                "VALIDATION_ERROR",
                                                "请求参数校验失败",
                                                Map.of("fieldErrors", fieldErrors),
                                                TraceIdUtils.get(request)));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<?>> handleConstraintViolation(
                        ConstraintViolationException exception,
                        HttpServletRequest request) {
                return response(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                ApiResponse.failure(
                                                "VALIDATION_ERROR",
                                                "请求参数校验失败",
                                                Map.of("violations", exception.getConstraintViolations()
                                                                .stream()
                                                                .map(violation -> violation.getMessage())
                                                                .toList()),
                                                TraceIdUtils.get(request)));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<?>> handleUnreadableMessage(
                        HttpMessageNotReadableException exception,
                        HttpServletRequest request) {
                return clientError(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                                "请求体格式不正确", request);
        }

        @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
        public ResponseEntity<ApiResponse<?>> handleBadRequest(
                        Exception exception,
                        HttpServletRequest request) {
                return clientError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                                "请求参数格式不正确", request);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException exception,
                        HttpServletRequest request) {
                return clientError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                                "请求方法不被支持", request);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(
                        HttpMediaTypeNotSupportedException exception,
                        HttpServletRequest request) {
                return clientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                                "请求内容类型不被支持", request);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
                        DataIntegrityViolationException exception,
                        HttpServletRequest request) {
                return response(
                                HttpStatus.CONFLICT,
                                ApiResponse.failure(
                                                "DATA_CONFLICT",
                                                "数据已存在或违反数据约束",
                                                TraceIdUtils.get(request)));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<?>> handleException(
                        Exception exception,
                        HttpServletRequest request) {

                log.error(
                                "请求处理失败，method={}, path={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                exception);
                                
                return response(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                ApiResponse.failure(
                                                "INTERNAL_ERROR",
                                                "服务器内部错误",
                                                TraceIdUtils.get(request)));
        }

        private ResponseEntity<ApiResponse<?>> clientError(
                        HttpStatus status,
                        String code,
                        String message,
                        HttpServletRequest request) {
                return response(status, ApiResponse.failure(code, message, TraceIdUtils.get(request)));
        }

        private ResponseEntity<ApiResponse<?>> response(
                        HttpStatus status,
                        ApiResponse<?> body) {
                return ResponseEntity.status(status).body(body);
        }
}
