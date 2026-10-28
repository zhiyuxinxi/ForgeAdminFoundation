package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.TaskSourceHandler;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskContractTest {
    @Test
    void taskErrorsUseBothInstalledLocaleBundlesWithoutChangingKeys() {
        var previous = LocaleContextHolder.getLocaleContext();
        try {
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertThat(TaskMessages.error(403, "permission.denied").getMessage()).isEqualTo("无权执行当前任务操作");
            LocaleContextHolder.setLocale(Locale.US);
            assertThat(TaskMessages.error(403, "permission.denied").getMessage())
                    .isEqualTo("The current task operation is not permitted");
        } finally {
            LocaleContextHolder.setLocaleContext(previous);
        }
    }

    @Test
    void sourceRegistrationRejectsDuplicateOwnersAndUnsupportedTransactions() {
        assertThatThrownBy(() -> new TaskSources(List.of(new Source("master"), new Source("master"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TaskSources(List.of(new Source("other"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new TaskSources(List.of()).require("missing"))
                .hasMessageContaining("任务来源");
    }

    private record Source(String transactionDataSource) implements TaskSourceHandler {
        @Override
        public String sourceType() { return "test.source"; }
        @Override
        public void authorize(String key, Actor actor, Operation operation) { }
        @Override
        public void validateCreation(com.mdframe.forge.task.api.TaskDefinition definition,
                                     com.mdframe.forge.task.api.BusinessTaskEngine.Create command, Actor actor) { }
        @Override
        public void validateCompletion(BusinessTask task, String result, Actor actor) { }
        @Override
        public void onTerminal(BusinessTask task, Actor actor) { }
    }
}
