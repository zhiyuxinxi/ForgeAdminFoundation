package com.mdframe.forge.starter.core.i18n;

import com.mdframe.forge.starter.core.config.UiLocaleAutoConfiguration;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UiMessagesTest {
    @AfterEach void resetLocale() { LocaleContextHolder.resetLocaleContext(); }

    @Test void responseMessagesChangeWithoutChangingCodesOrData() {
        LocaleContextHolder.setLocale(Locale.US);
        Map<String, String> data = Map.of("name", "操作成功", "status", "APPROVED");
        RespInfo<?> response = RespInfo.success(data);
        assertEquals(200, response.getCode());
        assertEquals("Operation successful", response.getMessage());
        assertSame(data, response.getData());
        assertEquals("客户原始内容", RespInfo.error("客户原始内容").getMessage());
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("操作成功", RespInfo.success().getMessage());
    }

    @Test void missingContextAndUnsupportedLanguageFallBackToChinese() {
        LocaleContextHolder.resetLocaleContext();
        assertEquals("操作失败", RespInfo.error().getMessage());
        LocaleContextHolder.setLocale(Locale.GERMAN);
        assertEquals("操作失败", RespInfo.error().getMessage());
    }

    @Test void headerResolverSupportsEnglishVariantsAndHasExplicitChineseFallback() {
        var resolver = new UiLocaleAutoConfiguration().localeResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-GB,en;q=0.8");
        assertEquals("en", resolver.resolveLocale(request).getLanguage());
        request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "de-DE");
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(request));
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(new MockHttpServletRequest()));
    }

    @Test void interpolatesParametersAndPreservesMissingEnglishLabels() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Missing required parameter: recordId", UiMessages.message("error.missingParameter", "recordId"));
        assertEquals("Pending", UiMessages.label("待处理", "Pending"));
        assertEquals("自定义标签", UiMessages.label("自定义标签", " "));
        assertNull(UiMessages.translate(null));
        assertEquals("Name", UiMessages.label("姓名", null));
        assertEquals("采购单不存在", RespInfo.error("采购单不存在").getMessage());
    }

    @Test void customerAuthenticationAndApprovalErrorsFollowTheRequestLocale() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("The current password is incorrect", RespInfo.error("旧密码错误").getMessage());
        assertEquals("Enter an approval comment", RespInfo.error("请输入审批意见").getMessage());
        assertEquals("Could not read encrypted fields. Contact your administrator to check the stored encryption key configuration",
                RespInfo.error("低代码加密字段读取失败，请检查持久化密钥配置").getMessage());
        assertEquals("File not found: 原文件", UiMessages.message("customer.fileMissing", "原文件"));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("旧密码错误", RespInfo.error("旧密码错误").getMessage());
        assertEquals("低代码加密字段读取失败，请检查持久化密钥配置", RespInfo.error("低代码加密字段读取失败，请检查持久化密钥配置").getMessage());
    }

    @Test void publicAdministrationErrorsAndValidationKeepValuesAndIdentifiers() throws Exception {
        LocaleContextHolder.setLocale(Locale.US);
        Map<String, String> original = Map.of("roleCode", "租户不存在", "memo", "客户原文");
        RespInfo<?> response = RespInfo.build(409, "租户不存在", original);
        assertEquals(409, response.getCode());
        assertEquals("Tenant does not exist", response.getMessage());
        assertSame(original, response.getData());
        var errors = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new org.springframework.validation.FieldError("request", "clientId", "客户端ID不能为空"));
        var result = new com.mdframe.forge.starter.core.exception.GlobalExceptionHandler()
                .handleBindException(new org.springframework.validation.BindException(errors), new MockHttpServletRequest());
        assertEquals(400, result.getCode());
        assertEquals("clientId: Client ID is required", result.getMessage());
        assertEquals("未知客户提示", RespInfo.error("未知客户提示").getMessage());
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("租户不存在", RespInfo.error("租户不存在").getMessage());
    }

    @Test void publicCatalogKeepsPlaceholdersAndLocalizesOnlyDeclaredMessageFragments() throws IOException {
        var chinese = loadCatalog("/forge/i18n/messages.properties");
        var english = loadCatalog("/forge/i18n/messages_en.properties");
        assertCatalogsMatch(chinese, english);
        var han = Pattern.compile("[\\p{IsHan}]");
        for (String key : english.stringPropertyNames()) {
            if (key.startsWith("public.")) assertFalse(han.matcher(english.getProperty(key)).find(), key);
        }
        LocaleContextHolder.setLocale(Locale.US);
        String originalValue = "租户不存在-{0}-原值";
        assertEquals("The field is not in the model: " + originalValue,
                UiMessages.translate("字段不在模型中: ") + originalValue);
        assertEquals("External identity [" + originalValue + "] is already bound to another user",
                cn.hutool.core.util.StrUtil.format(UiMessages.translate("外部身份[{}]已绑定其他用户"), originalValue));
        assertEquals(originalValue + "; 2 attempts remaining",
                String.format(UiMessages.translate("%s，还剩 %d 次尝试机会"), originalValue, 2));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("字段不在模型中: " + originalValue,
                UiMessages.translate("字段不在模型中: ") + originalValue);
    }

    @Test void loadsBusinessOwnedMessagesWithoutReplacingCoreMessagesOrCustomerData() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Business customer: 客户甲-{0}", UiMessages.message("business.test.customer", "客户甲-{0}"));
        assertEquals("Operation successful", UiMessages.message("operation.success"));
        assertEquals("未补充英文的业务提示", UiMessages.message("business.test.original"));
        assertEquals("business.missing", UiMessages.message("business.missing"));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("业务客户：客户甲", UiMessages.message("business.test.customer", "客户甲"));
    }

    @Test void tenantDatasourceMessagesTranslateThroughTheDeclaredChineseCatalog() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("The tenant business datasource is not granted or writable",
                UiMessages.translate("租户业务数据源未授权或不可写"));
        assertEquals("Create the tenant before granting a business datasource",
                UiMessages.translate("请先创建租户再授权业务数据源"));
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("租户业务数据源未授权或不可写", UiMessages.translate("租户业务数据源未授权或不可写"));
        assertEquals("请先创建租户再授权业务数据源", UiMessages.translate("请先创建租户再授权业务数据源"));
    }

    @Test void businessValidationAliasesReachTheErrorHandlerWithoutOverridingCoreOrData() {
        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Business test name is required", UiMessages.translate("业务测试名称不能为空"));
        var errors = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new org.springframework.validation.FieldError("request", "name", "业务测试名称不能为空"));
        var response = new com.mdframe.forge.starter.core.exception.GlobalExceptionHandler()
                .handleBindException(new org.springframework.validation.BindException(errors), new MockHttpServletRequest());
        assertEquals(400, response.getCode());
        assertEquals("name: Business test name is required", response.getMessage());

        Map<String, String> original = Map.of("name", "业务测试名称不能为空");
        RespInfo<?> businessError = RespInfo.build(409, "业务测试名称不能为空", original);
        assertEquals(409, businessError.getCode());
        assertEquals("Business test name is required", businessError.getMessage());
        assertSame(original, businessError.getData());
        assertEquals("业务测试名称不能为空", original.get("name"));
        assertEquals("Operation successful", UiMessages.translate("操作成功"));
        assertEquals("业务客户：{0}", UiMessages.translate("业务客户：{0}"));
        assertEquals("未登记的客户文本", UiMessages.translate("未登记的客户文本"));

        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("业务测试名称不能为空", UiMessages.translate("业务测试名称不能为空"));
        LocaleContextHolder.setLocale(Locale.GERMAN);
        assertEquals("业务测试名称不能为空", UiMessages.translate("业务测试名称不能为空"));
    }

    @Test void rawCatalogCheckRejectsMissingKeysThatResourceBundleFallbackHides() throws IOException {
        String chinese = "shared=共享\nmissingEnglish=仅中文\n";
        String english = "shared=Shared\n";
        var parent = new PropertyResourceBundle(new StringReader(chinese));
        var inherited = new InheritedEnglishBundle(new StringReader(english), parent);
        assertEquals(parent.keySet(), inherited.keySet());
        assertEquals("仅中文", inherited.getString("missingEnglish"));

        var missingEnglish = assertThrows(AssertionError.class,
                () -> assertCatalogsMatch(readCatalog(new StringReader(chinese)), readCatalog(new StringReader(english))));
        assertTrue(missingEnglish.getMessage().contains("missingEnglish"));
        var missingChinese = assertThrows(AssertionError.class,
                () -> assertCatalogsMatch(readCatalog(new StringReader(english)), readCatalog(new StringReader(chinese))));
        assertTrue(missingChinese.getMessage().contains("missingEnglish"));
    }

    @Test void rawCatalogCheckRejectsBlankValuesAndNonPublicPlaceholderChanges() throws IOException {
        var chinese = readCatalog(new StringReader("auth.example=原值 {0} {} %s %d\n"));
        for (String blank : new String[] {"", " ", "\t"}) {
            var empty = readCatalog(new StringReader("auth.example=" + blank + "\n"));
            assertThrows(AssertionError.class, () -> assertCatalogsMatch(chinese, empty));
            assertThrows(AssertionError.class, () -> assertCatalogsMatch(empty, chinese));
        }
        var missingParameter = readCatalog(new StringReader("auth.example=Original {0} {} %s\n"));
        assertThrows(AssertionError.class, () -> assertCatalogsMatch(chinese, missingParameter));
        var compatible = readCatalog(new StringReader("auth.example=Original {0} {} %s %d\n"));
        assertDoesNotThrow(() -> assertCatalogsMatch(chinese, compatible));
    }

    @Test void rawCatalogReaderRejectsDuplicateKeysInsteadOfSilentlyReplacingValues() {
        assertThrows(AssertionError.class,
                () -> readCatalog(new StringReader("auth.example=First\nauth.example=Second\n")));
    }

    private static Properties loadCatalog(String resource) throws IOException {
        try (var input = UiMessagesTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, "Missing catalog: " + resource);
            return readCatalog(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
    }

    private static Properties readCatalog(Reader reader) throws IOException {
        var catalog = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                assertFalse(containsKey(key), "Duplicate message key: " + key);
                return super.put(key, value);
            }
        };
        catalog.load(reader);
        return catalog;
    }

    private static void assertCatalogsMatch(Properties chinese, Properties english) {
        var chineseOnly = new TreeSet<>(chinese.stringPropertyNames());
        chineseOnly.removeAll(english.stringPropertyNames());
        var englishOnly = new TreeSet<>(english.stringPropertyNames());
        englishOnly.removeAll(chinese.stringPropertyNames());
        assertTrue(chineseOnly.isEmpty() && englishOnly.isEmpty(),
                () -> "Message keys differ; Chinese-only: " + chineseOnly + "; English-only: " + englishOnly);
        var placeholders = Pattern.compile("\\{(?:\\d+)?\\}|%(?:\\d+\\$)?[sd]");
        for (String key : chinese.stringPropertyNames()) {
            String zh = chinese.getProperty(key);
            String en = english.getProperty(key);
            assertFalse(key.isBlank(), "Blank message key");
            assertFalse(zh.isBlank(), "Blank Chinese value: " + key);
            assertFalse(en.isBlank(), "Blank English value: " + key);
            assertEquals(placeholders.matcher(zh).results().map(java.util.regex.MatchResult::group).toList(),
                    placeholders.matcher(en).results().map(java.util.regex.MatchResult::group).toList(), key);
        }
    }

    private static final class InheritedEnglishBundle extends PropertyResourceBundle {
        private InheritedEnglishBundle(Reader reader, ResourceBundle parent) throws IOException {
            super(reader);
            setParent(parent);
        }
    }
}
