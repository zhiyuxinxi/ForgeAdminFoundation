package com.mdframe.forge.starter.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.log.context.OperationAuditContext;

final class AuditSnapshotWriter {
    static void fillAuditSnapshot(OperationLogInfo logInfo, String requestParams, String responseResult, LogProperties logProperties) {
        OperationAuditContext.Snapshot snapshot = OperationAuditContext.snapshot();
        if (snapshot.getOperationContent() != null) {
            logInfo.setOperationContent(AuditEventText.truncate(AuditEventText.toJsonString(snapshot.getOperationContent()), 1000));
        }
        if (snapshot.getBeforeData() != null) {
            logInfo.setBeforeData(AuditEventText.truncate(AuditEventText.toJsonString(snapshot.getBeforeData()), logProperties.getResponseResultMaxLength()));
        }
        if (snapshot.getAfterData() != null) {
            logInfo.setAfterData(AuditEventText.truncate(AuditEventText.toJsonString(snapshot.getAfterData()), logProperties.getResponseResultMaxLength()));
        }
        if (snapshot.getDiffData() != null) {
            logInfo.setDiffData(AuditEventText.truncate(AuditEventText.toJsonString(snapshot.getDiffData()), logProperties.getResponseResultMaxLength()));
        }
        if (StrUtil.isBlank(logInfo.getAfterData()) && AuditOperationMetadata.isMutatingOperation(logInfo.getOperationType())) {
            String fallbackAfterData = resolveFallbackAfterData(requestParams, responseResult);
            if (StrUtil.isNotBlank(fallbackAfterData)) {
                logInfo.setAfterData(AuditEventText.truncate(fallbackAfterData, logProperties.getResponseResultMaxLength()));
            }
        }
    }

    static String resolveFallbackAfterData(String requestParams, String responseResult) {
        if (StrUtil.isNotBlank(requestParams) && !containsOmittedRequestBody(requestParams)) {
            return requestParams;
        }
        if (StrUtil.isNotBlank(responseResult) && !containsOmittedRequestBody(responseResult)) {
            return responseResult;
        }
        return "";
    }

    static boolean containsOmittedRequestBody(String value) {
        return StrUtil.isNotBlank(value) && value.contains(AuditRequestCapture.DECRYPTED_REQUEST_BODY_OMITTED);
    }
}
