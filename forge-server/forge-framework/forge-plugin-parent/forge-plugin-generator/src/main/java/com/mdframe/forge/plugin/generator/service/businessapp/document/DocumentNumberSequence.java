package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRulePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.id.service.ISequenceService;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Tenant/object scoped sequence allocation and runtime number rendering. */
public final class DocumentNumberSequence {

    private final ISequenceService sequenceService;
    private final DocumentNumberTemplate templates;

    public DocumentNumberSequence(ISequenceService sequenceService, DocumentNumberTemplate templates) {
        this.sequenceService = sequenceService;
        this.templates = templates;
    }

    public String generateRuntimeDocumentNo(RuntimeDocumentNumber config, Map<String, Object> recordData, Long tenantId) {
        if (config == null || !Integer.valueOf(1).equals(config.documentEnabled())) {
            return null;
        }
        String template = templates.normalizeNoRuleTemplate(config.rawNoRule());
        if (StringUtils.isBlank(template)) {
            return null;
        }
        long sequence = sequenceService.nextId(buildNoRuleSequenceKey(config, template, tenantId));
        LoginUser loginUser = safeLoginUser();
        String starter = loginUser == null
                ? "starter"
                : StringUtils.firstNonBlank(loginUser.getUsername(), loginUser.getRealName(),
                        loginUser.getUserId() == null ? null : String.valueOf(loginUser.getUserId()), "starter");
        String deptCode = loginUser == null
                ? "DEPT"
                : StringUtils.firstNonBlank(
                        loginUser.getMainOrgId() == null ? null : String.valueOf(loginUser.getMainOrgId()),
                        loginUser.getDeptName(),
                        "DEPT");
        BusinessDocumentNoRulePreviewVO rendered = templates.renderNoRulePreview(
                template,
                StringUtils.defaultIfBlank(config.suiteCode(), "SUITE"),
                StringUtils.defaultIfBlank(config.objectCode(), "OBJECT"),
                starter,
                deptCode,
                recordData,
                sequence);
        if (!rendered.getErrors().isEmpty()) {
            String message = rendered.getErrors().stream()
                    .map(BusinessDocumentNoRulePreviewVO.PreviewIssueVO::getMessage)
                    .findFirst()
                    .orElse("单据编号规则不正确");
            throw new BusinessException(message);
        }
        return rendered.getPreviewNo();
    }

    public String generateDocumentNo(AiBusinessDocumentConfig config, Map<String, Object> recordData, Long tenantId) {
        RuntimeDocumentNumber number = config == null ? null : new RuntimeDocumentNumber(
                config.getTenantId(), config.getSuiteCode(), config.getObjectCode(), config.getDocumentEnabled(),
                config.getDocumentNoRule(), null);
        return generateRuntimeDocumentNo(number, recordData, tenantId);
    }

    private String buildNoRuleSequenceKey(RuntimeDocumentNumber config, String template, Long resolvedTenantId) {
        String tenantId = config == null || config.tenantId() == null ? String.valueOf(resolvedTenantId) : String.valueOf(config.tenantId());
        String suiteCode = config == null ? "SUITE" : StringUtils.defaultIfBlank(config.suiteCode(), "SUITE");
        String objectCode = config == null ? "OBJECT" : StringUtils.defaultIfBlank(config.objectCode(), "OBJECT");
        String period = resolveNoRuleSequencePeriod(template);
        String templateHash = Integer.toHexString(StringUtils.defaultString(template).hashCode());
        return "lowcode:document-no:" + tenantId + ":" + safeSequencePart(suiteCode) + ":"
                + safeSequencePart(objectCode) + ":" + period + ":" + templateHash;
    }

    private String resolveNoRuleSequencePeriod(String template) {
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.contains(template, "yyyyMMdd")) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (StringUtils.contains(template, "yyyyMM")) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        }
        if (StringUtils.contains(template, "yyyy")) {
            return now.format(DateTimeFormatter.ofPattern("yyyy"));
        }
        return "all";
    }

    private String safeSequencePart(String value) {
        String result = StringUtils.defaultIfBlank(value, "NA").replaceAll("[^A-Za-z0-9_\\-]", "_");
        return StringUtils.defaultIfBlank(result, "NA");
    }

    private LoginUser safeLoginUser() {
        try {
            return SessionHelper.getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }
}
