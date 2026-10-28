package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserImportSecurityContractTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void importEndpointShouldLogInternalExceptionsAndReturnStableMessages() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/controller/SysUserController.java"));
        String importMethod = source.substring(
                source.indexOf("public RespInfo<ImportResult<?>> importUsers"),
                source.indexOf("private SysUserDTO buildUserDtoFromRow"));

        assertThat(importMethod)
                .contains("log.error(\"导入用户第{}行写入失败\"",
                        "error.setErrorMessage(UiMessages.translate(\"用户数据写入失败\"))",
                        "log.error(\"用户导入失败\", e)",
                        "result.setSummary(ImportResult.PUBLIC_FAILURE_MESSAGE)")
                .doesNotContain("e.getMessage()", "exception.getMessage()");
    }

    @Test
    void localizedWriteFailureShouldUseTheRequestLocale() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertThat(UiMessages.translate("用户数据写入失败")).isEqualTo("用户数据写入失败");

        LocaleContextHolder.setLocale(Locale.US);
        assertThat(UiMessages.translate("用户数据写入失败")).isEqualTo("Could not save user data");
    }
}
