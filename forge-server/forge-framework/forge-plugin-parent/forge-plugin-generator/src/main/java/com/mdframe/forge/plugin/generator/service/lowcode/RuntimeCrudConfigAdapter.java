package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.lowcode.api.config.RuntimeCrudConfigSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.runtime.service.task.RuntimeTaskLinkFields;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Design-side adapter. It resolves admission and snapshots before copying runtime values. */
@Component
public final class RuntimeCrudConfigAdapter implements RuntimeCrudConfigSource {
    private static final ObjectMapper TASK_OPTIONS = new ObjectMapper();
    private final AiCrudConfigService configService;

    public RuntimeCrudConfigAdapter(AiCrudConfigService configService) {
        this.configService = configService;
    }

    public LowcodeRuntimeConfig load(String configKey) {
        return fromDesign(loadLegacy(configKey));
    }

    public LowcodeRuntimeConfig loadAllowDraft(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException(UiMessages.translate("CRUD配置不存在或已停用: ") + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException(UiMessages.translate("该配置不是配置驱动模式: ") + configKey);
        }
        return fromDesign(config);
    }

    public AiCrudConfig loadLegacy(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException(UiMessages.translate("CRUD配置不存在或已停用: ") + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException(UiMessages.translate("该配置不是配置驱动模式: ") + configKey);
        }
        if ("LOWCODE".equals(config.getBuildMode())
                && !"PUBLISHED".equals(config.getPublishStatus())
                && !isAuthorizedDesignPreviewRequest()) {
            throw new BusinessException(UiMessages.translate("低代码应用尚未发布: ") + configKey);
        }
        if (isAuthorizedDesignPreviewRequest()) {
            assertTaskPreviewReadOnly(config);
            return config;
        }
        return configService.resolvePublishedRuntimeConfig(config);
    }

    private void assertTaskPreviewReadOnly(AiCrudConfig draft) {
        if (draft.getPublishedVersion() == null || draft.getPublishedVersion() <= 0
                || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request)
                || "GET".equals(request.getRequest().getMethod()) || "HEAD".equals(request.getRequest().getMethod())) {
            return;
        }
        AiCrudConfig published = configService.resolvePublishedRuntimeConfig(draft);
        if (published != null && RuntimeTaskLinkFields.hasBindings(fromDesign(published), TASK_OPTIONS)) {
            throw new BusinessException(409, UiMessages.label("关联任务的业务记录请通过已发布入口修改，不能使用设计预览写入",
                    "Use the published entry to edit task-linked records; design preview writes are not allowed"));
        }
    }

    private boolean isAuthorizedDesignPreviewRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || !"1".equals(attributes.getRequest().getParameter("designPreview"))) {
            return false;
        }
        return configService.hasDesignPreviewPermission();
    }

    public static LowcodeRuntimeConfig fromDesign(AiCrudConfig source) {
        if (source == null) {
            return null;
        }
        LowcodeRuntimeConfig target = new LowcodeRuntimeConfig();
        target.setTenantId(source.getTenantId());
        target.setConfigKey(source.getConfigKey());
        target.setTableName(source.getTableName());
        target.setStatus(source.getStatus());
        target.setMode(source.getMode());
        target.setBuildMode(source.getBuildMode());
        target.setPublishStatus(source.getPublishStatus());
        target.setLayoutType(source.getLayoutType());
        target.setDomainCode(source.getDomainCode());
        target.setObjectCode(source.getObjectCode());
        target.setSearchSchema(source.getSearchSchema());
        target.setColumnsSchema(source.getColumnsSchema());
        target.setEditSchema(source.getEditSchema());
        target.setOptions(source.getOptions());
        target.setModelSchema(source.getModelSchema());
        target.setPageSchema(source.getPageSchema());
        target.setDesensitizeConfig(source.getDesensitizeConfig());
        target.setEncryptConfig(source.getEncryptConfig());
        target.setTransConfig(source.getTransConfig());
        target.setRuntimeDatasourceId(source.getRuntimeDatasourceId());
        target.setRuntimeDatasourceCode(source.getRuntimeDatasourceCode());
        target.setRuntimeDatasourceSnapshot(source.getRuntimeDatasourceSnapshot());
        target.setRuntimeTableName(source.getRuntimeTableName());
        target.setPrimaryKeyField(source.getPrimaryKeyField());
        target.setPrimaryKeyColumn(source.getPrimaryKeyColumn());
        target.setPrimaryKeyType(source.getPrimaryKeyType());
        target.setTenantStrategy(source.getTenantStrategy());
        target.setAuditStrategy(source.getAuditStrategy());
        target.setLogicDeleteStrategy(source.getLogicDeleteStrategy());
        target.setAppName(source.getAppName());
        target.setTableComment(source.getTableComment());
        return target;
    }
}
