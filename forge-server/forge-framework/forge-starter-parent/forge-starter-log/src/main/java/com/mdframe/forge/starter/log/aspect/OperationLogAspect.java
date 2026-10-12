package com.mdframe.forge.starter.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.log.service.ILogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 操作日志切面
 * 拦截@OperationLog注解，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ILogService.class)
public class OperationLogAspect {

    private final ILogService logService;
    private final LogProperties logProperties;
    @Qualifier("logTaskExecutor")
    private final Executor logTaskExecutor;
    private final IApiConfigManager apiConfigManager;

    private static final DateTimeFormatter TRACE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String TRACE_ID_KEY = "traceId";
    private static final Set<String> SENSITIVE_CREDENTIAL_PATH_SUFFIXES = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/changePassword",
            "/auth/resetPassword",
            "/auth/online/kickout",
            "/auth/online/batchKickout",
            "/oauth2/token",
            "/oauth2/revoke"
    );
    private static final String CAPABILITY_OAUTH_PATH = "/oauth2/";
    private static final String CAPABILITY_OPEN_GATEWAY_PATH = "/openapi/v1/capabilities/";

    /**
     * 定义切点：拦截所有标注@OperationLog的方法
     */
    //@Pointcut("@annotation(com.mdframe.forge.starter.core.annotation.log.OperationLog)")
    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否启用操作日志
        if (logProperties.getEnableOperationLog() == null || !logProperties.getEnableOperationLog()) {
            return joinPoint.proceed();
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String requestUrl = request.getRequestURI();

        // 检查是否在排除路径中
        if (isExcludePath(requestUrl)) {
            return joinPoint.proceed();
        }

        // 构建日志对象
        OperationLogInfo logInfo = new OperationLogInfo();
        AuditRequestCapture.populate(logInfo, request);

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        boolean decryptEndpoint = AuditRequestCapture.isApiDecryptEndpoint(joinPoint, method);

        resolveMetadata(logInfo, annotation, request, requestUrl);

        if (AuditOperationMetadata.shouldSkipOperationLog(logInfo.getOperationType(), request.getMethod())) {
            return proceedWithoutOperationLog(joinPoint);
        }

        // 生成traceId并放入MDC
        String traceId = generateTraceId();
        MDC.put(TRACE_ID_KEY, traceId);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        String requestParams = captureParameters(joinPoint, request, decryptEndpoint, annotation, logInfo);

        logInfo.setOperationContent(AuditOperationMetadata.buildOperationContent(logInfo, requestUrl));
        captureRequestIdentity(logInfo);

        printRequestIfEnabled(methodName, requestUrl, annotation, requestParams);

        // 执行目标方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            recordSuccess(logInfo, annotation, result, requestParams, startTime, methodName, requestUrl);

        } catch (Throwable e) {
            recordFailure(logInfo, annotation, e, requestParams, startTime, methodName, requestUrl);

            throw e;
        } finally {
            // 异步保存日志
            saveLogAsync(logInfo);
            OperationAuditContext.clear();
            // 清除MDC
            MDC.remove(TRACE_ID_KEY);
        }

        return result;
    }

    private void resolveMetadata(OperationLogInfo logInfo, OperationLog annotation,
                                 HttpServletRequest request, String requestUrl) {
        ApiConfigInfo apiConfig = null;
        if (annotation != null) {
            logInfo.setOperationModule(annotation.module());
            logInfo.setOperationType(annotation.type().name());
            logInfo.setOperationDesc(annotation.desc());
        } else {
            apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
            if (apiConfig != null) {
                logInfo.setOperationModule(apiConfig.getModuleCode());
                logInfo.setOperationType(AuditOperationMetadata.resolveDefaultOperationType(apiConfig.getReqMethod(), requestUrl));
                logInfo.setOperationDesc(apiConfig.getApiName());
            } else {
                logInfo.setOperationType(AuditOperationMetadata.resolveDefaultOperationType(request.getMethod(), requestUrl));
            }
        }
        AuditOperationMetadata.normalizeAuditMetadata(logInfo, requestUrl);
    }

    private String captureParameters(ProceedingJoinPoint joinPoint, HttpServletRequest request,
                                     boolean decryptEndpoint, OperationLog annotation, OperationLogInfo logInfo) {
        String requestParams = "";
        // 保存请求参数
        if (annotation != null) {
            if (annotation.saveRequestParams()) {
                requestParams = AuditRequestCapture.getRequestParams(joinPoint, request, decryptEndpoint);
                logInfo.setRequestParams(AuditEventText.truncate(requestParams, logProperties.getRequestParamsMaxLength()));
            }
        } else {
            requestParams = AuditRequestCapture.getRequestParams(joinPoint, request, decryptEndpoint);
            logInfo.setRequestParams(AuditEventText.truncate(requestParams, logProperties.getRequestParamsMaxLength()));
        }
        return requestParams;
    }

    private void printRequestIfEnabled(String methodName, String requestUrl,
                                       OperationLog annotation, String requestParams) {
        // 打印入参日志
        if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
            printRequestLog(methodName, requestUrl,
                    annotation != null ? annotation.module() : "",
                    annotation != null ? annotation.desc() : "",
                    requestParams);
        }
    }

    private void recordSuccess(OperationLogInfo logInfo, OperationLog annotation, Object result,
                               String requestParams, long startTime, String methodName, String requestUrl) {
        logInfo.setOperationStatus(1);  // 成功

        // 保存响应结果
        String responseResult = "";
        if (annotation != null && annotation.saveResponseResult() && result != null) {
            responseResult = JSONObject.toJSONString(result);
            logInfo.setResponseResult(AuditEventText.truncate(responseResult, logProperties.getResponseResultMaxLength()));
        }
        AuditSnapshotWriter.fillAuditSnapshot(logInfo, requestParams, responseResult, logProperties);

        // 计算执行时长
        long executeTime = System.currentTimeMillis() - startTime;
        logInfo.setExecuteTime(executeTime);

        // 打印出参日志
        if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
            printResponseLog(methodName, requestUrl,
                    annotation != null ? annotation.module() : "",
                    annotation != null ? annotation.desc() : "",
                    responseResult, executeTime);
        }
    }

    private void recordFailure(OperationLogInfo logInfo, OperationLog annotation, Throwable e,
                               String requestParams, long startTime, String methodName, String requestUrl) {
        logInfo.setOperationStatus(0);  // 失败
        logInfo.setErrorMsg(AuditEventText.truncate(e.getMessage(), 500));
        AuditSnapshotWriter.fillAuditSnapshot(logInfo, requestParams, null, logProperties);

        // 计算执行时长
        long executeTime = System.currentTimeMillis() - startTime;
        logInfo.setExecuteTime(executeTime);

        // 打印异常日志
        if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
            printErrorLog(methodName, requestUrl,
                    annotation != null ? annotation.module() : "",
                    annotation != null ? annotation.desc() : "",
                    e, executeTime);
        }
    }

    private Object proceedWithoutOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } finally {
            OperationAuditContext.clear();
        }
    }

    /**
     * 判断是否在排除路径中
     */
    private boolean isExcludePath(String requestUrl) {
        String normalizedUrl = requestUrl.replaceAll(";[^/]*", "").replaceAll("/+$", "");
        if (SENSITIVE_CREDENTIAL_PATH_SUFFIXES.stream().anyMatch(normalizedUrl::endsWith)
                || normalizedUrl.contains(CAPABILITY_OAUTH_PATH)
                || normalizedUrl.contains(CAPABILITY_OPEN_GATEWAY_PATH)) {
            return true;
        }
        String[] excludePaths = logProperties.getExcludePaths();
        if (excludePaths == null || excludePaths.length == 0) {
            return false;
        }
        for (String path : excludePaths) {
            if (requestUrl.matches(path.replace("**", ".*").replace("*", "[^/]*"))) {
                return true;
            }
        }
        return false;
    }

    private void captureRequestIdentity(OperationLogInfo logInfo) {
        try {
            LoginUser user = SessionHelper.getLoginUser();
            if (user != null) {
                // Copy scalar values before the request changes its session or leaves this thread.
                logInfo.setUserId(user.getUserId());
                logInfo.setTenantId(user.getTenantId());
                logInfo.setUsername(user.getUsername());
                logInfo.setOperatorName(StrUtil.blankToDefault(user.getRealName(), user.getUsername()));
            }
        } catch (Exception e) {
            log.debug("获取操作日志请求身份失败", e);
        }
    }

    /**
     * 异步保存日志
     */
    private void saveLogAsync(OperationLogInfo logInfo) {
        try {
            // 使用线程池异步保存日志，避免影响业务性能
            logTaskExecutor.execute(() -> {
                try {
                    logService.saveOperationLog(logInfo);
                } catch (Exception e) {
                    log.error("保存操作日志失败", e);
                }
            });
        } catch (Exception e) {
            log.error("提交日志保存任务失败", e);
        }
    }

    /**
     * 生成traceId
     */
    private String generateTraceId() {
        String timestamp = LocalDateTime.now().format(TRACE_ID_FORMATTER);
        String random = String.valueOf(Math.abs(System.nanoTime() % 1000000));
        return timestamp + random + "_LOG";
    }

    /**
     * 打印入参日志
     */
    private void printRequestLog(String methodName, String requestUrl, String module, String desc, String params) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【入参日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            if (StrUtil.isNotBlank(params)) {
                logMsg.append(", 请求参数: ").append(params);
            }
            log.info(logMsg.toString());
        } catch (Exception e) {
            log.debug("打印入参日志失败", e);
        }
    }

    /**
     * 打印出参日志
     */
    private void printResponseLog(String methodName, String requestUrl, String module, String desc, String response, long executeTime) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【出参日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            if (StrUtil.isNotBlank(response)) {
                logMsg.append(", 返回参数: ").append(response);
            }
            logMsg.append(", 耗时: ").append(executeTime).append("ms");
            log.info(logMsg.toString());
        } catch (Exception e) {
            log.debug("打印出参日志失败", e);
        }
    }

    /**
     * 打印异常日志
     */
    private void printErrorLog(String methodName, String requestUrl, String module, String desc, Throwable e, long executeTime) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【异常日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            logMsg.append(", 耗时: ").append(executeTime).append("ms");
            log.error(logMsg.toString(), e);
        } catch (Exception ex) {
            log.debug("打印异常日志失败", ex);
        }
    }
}
