package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;

import java.util.ResourceBundle;

final class TaskMessages {
    private TaskMessages() { }

    static BusinessException error(int code, String key) {
        var bundle = ResourceBundle.getBundle("forge.i18n.task.messages", UiMessages.locale(),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return new BusinessException(code, bundle.getString(key));
    }
}
