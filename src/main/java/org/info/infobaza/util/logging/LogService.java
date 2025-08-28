package org.info.infobaza.util.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.info.infobaza.util.user.UserUtil.getClientIpAddress;
import static org.info.infobaza.util.user.UserUtil.getCurrentHttpRequest;

@Aspect
@Component
public class LogService {
    private static final Logger logger = LogManager.getLogger(LogService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Around("@annotation(org.info.infobaza.util.logging.LogRequest)")
    public Object logControllerCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get request details
        HttpServletRequest request = getCurrentHttpRequest();
        String controllerName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String clientIp = getClientIpAddress(request);

        String requestParams = Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    try {
                        return Arrays.stream(arg.getClass().getDeclaredFields())
                                .filter(field -> field.isAnnotationPresent(Loggable.class))
                                .map(field -> {
                                    try {
                                        field.setAccessible(true);
                                        Object value = field.get(arg);
                                        String fieldName = field.getAnnotation(Loggable.class).name();
                                        if (fieldName.isEmpty()) {
                                            fieldName = field.getName();
                                        }
                                        return String.format("%s=\"%s\"", fieldName, value != null ? value.toString() : "null");
                                    } catch (IllegalAccessException e) {
                                        return "error_accessing_field";
                                    }
                                })
                                .collect(Collectors.joining(", "));
                    } catch (Exception e) {
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining("; "));

        String requestDetails = String.format("Controller: %s, Method: %s, HTTP: %s, URI: %s%s, ClientIP: %s, Args: [%s]",
                controllerName, methodName, httpMethod, uri, queryString, clientIp, requestParams);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        long startTime = System.currentTimeMillis();

        try {
            Object response = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logger.info("{} - Request: {}, ExecutionTime: {}ms",
                    timestamp, requestDetails, executionTime);

            return response;
        } catch (Throwable t) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("{} - Request: {}, Error: {}, ExecutionTime: {}ms",
                    timestamp, requestDetails, t.getMessage(), executionTime);
            throw t;
        }
    }


}