package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.dto.ResetUserPasswordDTO;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.auth.domain.ChangePasswordRequest;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysUserPasswordControllerTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resetPasswordAcceptsCredentialsOnlyFromJsonBody() throws Exception {
        ISysUserService userService = mock(ISysUserService.class);
        when(userService.resetPassword(76L, "replacement-value")).thenReturn(true);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SysUserController(userService)).build();

        mockMvc.perform(post("/system/user/resetPwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":76,\"password\":\"replacement-value\"}"))
                .andExpect(status().isOk());

        verify(userService).resetPassword(76L, "replacement-value");
    }

    @Test
    void resetPasswordRejectsLegacyQueryParameterTransport() throws Exception {
        ISysUserService userService = mock(ISysUserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SysUserController(userService)).build();

        mockMvc.perform(post("/system/user/resetPwd")
                        .param("id", "76")
                        .param("password", "replacement-value"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).resetPassword(anyLong(), anyString());
    }

    @Test
    void passwordValidationMessagesResolveThroughTheSharedUiCatalog() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            LocaleContextHolder.setLocale(Locale.US);

            var resetMessages = validator.validate(new ResetUserPasswordDTO()).stream()
                    .map(violation -> UiMessages.translate(violation.getMessage()))
                    .toList();
            assertEquals(true, resetMessages.contains("User is required"));
            assertEquals(true, resetMessages.contains("Password is required"));

            var changeMessages = validator.validate(new ChangePasswordRequest()).stream()
                    .map(violation -> UiMessages.translate(violation.getMessage()))
                    .toList();
            assertEquals(true, changeMessages.contains("Current password is required"));
            assertEquals(true, changeMessages.contains("New password is required"));
        }
    }
}
