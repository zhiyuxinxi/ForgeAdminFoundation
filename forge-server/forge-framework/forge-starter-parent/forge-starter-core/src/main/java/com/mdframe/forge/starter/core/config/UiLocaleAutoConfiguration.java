package com.mdframe.forge.starter.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/** Explicit Chinese fallback independent of the host OS language. */
@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration")
public class UiLocaleAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setSupportedLocales(List.of(Locale.SIMPLIFIED_CHINESE, Locale.US, Locale.CHINESE, Locale.ENGLISH));
        return resolver;
    }
}
