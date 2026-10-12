package com.mdframe.forge.starter.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestBody;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
final class AuditRequestCapture {
    static final String DECRYPTED_REQUEST_BODY_OMITTED = "[DECRYPTED_REQUEST_BODY_OMITTED]";

    static String getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request, boolean redactRequestBody) {
        try {
            Map<String, Object> params = new HashMap<>();

            addArguments(params, joinPoint, redactRequestBody);

            // 获取URL参数
            request.getParameterMap().forEach((key, value) -> {
                if (value != null && value.length > 0) {
                    params.put(key, value.length == 1 ? value[0] : value);
                }
            });

            return JSONObject.toJSONString(params);
        } catch (Exception e) {
            log.error("获取请求参数失败", e);
            return "";
        }
    }

    static boolean isApiDecryptEndpoint(ProceedingJoinPoint joinPoint, Method method) {
        if (method.isAnnotationPresent(ApiDecrypt.class)
                || method.getDeclaringClass().isAnnotationPresent(ApiDecrypt.class)) {
            return true;
        }
        Object target = joinPoint.getTarget();
        return target != null && target.getClass().isAnnotationPresent(ApiDecrypt.class);
    }

    static boolean hasParameterAnnotation(Method method, int parameterIndex, Class<? extends Annotation> annotationClass) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (parameterIndex < 0 || parameterIndex >= annotations.length) {
            return false;
        }
        for (Annotation annotation : annotations[parameterIndex]) {
            if (annotationClass.isInstance(annotation)) {
                return true;
            }
        }
        return false;
    }

    static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP的情况，取第一个
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    static String decodeHeader(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static void addArguments(Map<String, Object> params, ProceedingJoinPoint joinPoint, boolean redactRequestBody) {
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                // 过滤掉Request、Response等对象
                if (arg instanceof HttpServletRequest
                        || arg instanceof jakarta.servlet.http.HttpServletResponse
                        || arg instanceof org.springframework.web.multipart.MultipartFile) {
                    continue;
                }
                String paramName = parameterNames != null && i < parameterNames.length
                        ? parameterNames[i]
                        : "arg" + i;
                if (redactRequestBody && hasParameterAnnotation(signature.getMethod(), i, RequestBody.class)) {
                    params.put(paramName, DECRYPTED_REQUEST_BODY_OMITTED);
                } else {
                    params.put(paramName, arg);
                }
            }
        }

    }

    static void populate(OperationLogInfo event, HttpServletRequest request) {
        event.setOperationTime(java.time.LocalDateTime.now());
        event.setRequestUrl(request.getRequestURI());
        event.setRequestMethod(request.getMethod());
        event.setOperationIp(getClientIp(request));
        event.setUserAgent(request.getHeader("User-Agent"));
        event.setOperationPage(AuditEventText.truncate(request.getHeader("X-Page-Path"), 500));
        event.setOperationPageTitle(AuditEventText.truncate(decodeHeader(request.getHeader("X-Page-Title")), 200));
    }

}
