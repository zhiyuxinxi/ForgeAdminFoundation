package com.mdframe.forge.starter.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;

final class AuditOperationMetadata {
    static boolean isMutatingOperation(String operationType) {
        return "ADD".equals(operationType)
                || "UPDATE".equals(operationType)
                || "DELETE".equals(operationType)
                || "IMPORT".equals(operationType)
                || "EXPORT".equals(operationType)
                || "OTHER".equals(operationType);
    }

    static boolean shouldSkipOperationLog(String operationType, String requestMethod) {
        if ("QUERY".equals(operationType)) {
            return true;
        }
        if (isMutatingOperation(operationType)) {
            return false;
        }
        return isReadOnlyMethod(requestMethod);
    }

    static boolean isReadOnlyMethod(String requestMethod) {
        return "GET".equalsIgnoreCase(requestMethod)
                || "HEAD".equalsIgnoreCase(requestMethod)
                || "OPTIONS".equalsIgnoreCase(requestMethod);
    }

    static String resolveDefaultOperationType(String requestMethod, String requestUrl) {
        if (isReadOnlyMethod(requestMethod)) {
            return "QUERY";
        }
        if (isQueryLikeRequest(requestUrl)) {
            return "QUERY";
        }
        if ("DELETE".equalsIgnoreCase(requestMethod) || isDeleteLikeRequest(requestUrl)) {
            return "DELETE";
        }
        if (isAddLikeRequest(requestUrl)) {
            return "ADD";
        }
        return "UPDATE";
    }

    static boolean isQueryLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/page")
                || normalizedUrl.endsWith("/list")
                || normalizedUrl.endsWith("/tree")
                || normalizedUrl.endsWith("/detail")
                || normalizedUrl.endsWith("/getbyid")
                || normalizedUrl.endsWith("/options")
                || normalizedUrl.endsWith("/profile")
                || normalizedUrl.endsWith("/query");
    }

    static boolean isDeleteLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/remove")
                || normalizedUrl.endsWith("/removebatch")
                || normalizedUrl.endsWith("/delete")
                || normalizedUrl.endsWith("/deletebatch");
    }

    static boolean isAddLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/add")
                || normalizedUrl.endsWith("/create")
                || normalizedUrl.endsWith("/import");
    }

    static String normalizeRequestUrl(String requestUrl) {
        if (StrUtil.isBlank(requestUrl)) {
            return "";
        }
        return requestUrl.trim().toLowerCase();
    }

    static void normalizeAuditMetadata(OperationLogInfo logInfo, String requestUrl) {
        if (StrUtil.isBlank(logInfo.getOperationModule())) {
            String pageTitle = StrUtil.blankToDefault(logInfo.getOperationPageTitle(), "");
            if (StrUtil.isNotBlank(pageTitle) && !isGenericPageTitle(pageTitle)) {
                logInfo.setOperationModule(pageTitle);
            }
        }
        if (StrUtil.isBlank(logInfo.getOperationDesc())) {
            logInfo.setOperationDesc(resolveDefaultOperationDesc(logInfo.getOperationType(), requestUrl));
        }
    }

    static boolean isGenericPageTitle(String pageTitle) {
        return "企业级中后台管理系统".equals(pageTitle)
                || "企业级中后台基础框架".equals(pageTitle);
    }

    static String resolveDefaultOperationDesc(String operationType, String requestUrl) {
        if ("ADD".equals(operationType)) {
            return "新增数据";
        }
        if ("UPDATE".equals(operationType)) {
            return "修改数据";
        }
        if ("DELETE".equals(operationType)) {
            return "删除数据";
        }
        if ("IMPORT".equals(operationType)) {
            return "导入数据";
        }
        if ("EXPORT".equals(operationType)) {
            return "导出数据";
        }
        return StrUtil.blankToDefault(requestUrl, "执行操作");
    }

    static String buildOperationContent(OperationLogInfo logInfo, String requestUrl) {
        String desc = StrUtil.blankToDefault(logInfo.getOperationDesc(), "");
        String module = StrUtil.blankToDefault(logInfo.getOperationModule(), "");
        String type = StrUtil.blankToDefault(logInfo.getOperationType(), "");
        String pageTitle = StrUtil.blankToDefault(logInfo.getOperationPageTitle(), "");
        StringBuilder content = new StringBuilder();
        if (StrUtil.isNotBlank(pageTitle)) {
            content.append("页面[").append(pageTitle).append("] ");
        }
        if (StrUtil.isNotBlank(module)) {
            content.append("模块[").append(module).append("] ");
        }
        if (StrUtil.isNotBlank(type)) {
            content.append("类型[").append(type).append("] ");
        }
        if (StrUtil.isNotBlank(desc)) {
            content.append(desc);
        } else {
            content.append(requestUrl);
        }
        return AuditEventText.truncate(content.toString(), 1000);
    }
}
