package com.mdframe.forge.plugin.system.auth;

import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginCaptchaPolicyResolverAuthMethodsTest {
    @Test void loginConfigAndExecutionRequireCaptchaWhenClientDisallowsPasswordOnly() {
        ConfigManagerService configs = mock(ConfigManagerService.class);
        IClientService clients = mock(IClientService.class);
        LoginConfig global = new LoginConfig();
        global.setEnableCaptcha(false);
        when(configs.getLoginConfig()).thenReturn(global);
        SysClient client = new SysClient();
        client.setAuthTypes("password_captcha, oauth2");
        when(clients.getByCode("pc")).thenReturn(client);
        LoginCaptchaPolicyResolver resolver = new LoginCaptchaPolicyResolver(configs, clients);

        assertThat(resolver.resolve("pc").getEnableCaptcha()).isTrue();
        client.setAuthTypes("password,password_captcha");
        assertThat(resolver.resolve("pc").getEnableCaptcha()).isFalse();
    }
}
