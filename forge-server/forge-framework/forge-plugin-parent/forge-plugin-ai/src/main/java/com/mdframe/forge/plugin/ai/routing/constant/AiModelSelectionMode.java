package com.mdframe.forge.plugin.ai.routing.constant;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.util.StringUtils;

public enum AiModelSelectionMode {
    PINNED, POLICY;

    public static AiModelSelectionMode fromNullable(String value) {
        if (!StringUtils.hasText(value)) return PINNED;
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BusinessException(UiMessages.translate("未知的模型选择模式: ") + value); }
    }
}
