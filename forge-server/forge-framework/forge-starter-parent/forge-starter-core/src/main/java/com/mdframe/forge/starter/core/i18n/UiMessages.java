package com.mdframe.forge.starter.core.i18n;

import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** Localizes declared display text only; business values and user content stay unchanged. */
public final class UiMessages {
    private static final String BASENAME = "forge.i18n.messages";
    private static final ResourceBundle CHINESE = ResourceBundle.getBundle(BASENAME, Locale.ROOT);
    private static final ResourceBundle ENGLISH = ResourceBundle.getBundle(BASENAME, Locale.ENGLISH);
    // The business module owns this distinct bundle; it never shadows the core basename.
    private static final String BUSINESS_BASENAME = "forge.i18n.business.messages";
    private static final ResourceBundle BUSINESS_CHINESE = businessBundle(Locale.ROOT);
    private static final ResourceBundle BUSINESS_ENGLISH = businessBundle(Locale.ENGLISH);
    private static final Map<String, String> LEGACY_KEYS = legacyKeys();
    private static final Map<String, String> BUSINESS_DISPLAY_KEYS = businessDisplayKeys();

    private UiMessages() {}

    public static Locale locale() {
        return LocaleContextHolder.getLocaleContext() != null && "en".equals(LocaleContextHolder.getLocale().getLanguage())
                ? Locale.US : Locale.SIMPLIFIED_CHINESE;
    }

    public static String message(String key, Object... arguments) {
        boolean english = "en".equals(locale().getLanguage());
        ResourceBundle core = english ? ENGLISH : CHINESE;
        ResourceBundle bundle = core.containsKey(key) ? core : key.startsWith("business.")
                ? (english ? BUSINESS_ENGLISH : BUSINESS_CHINESE) : null;
        String pattern = bundle != null && bundle.containsKey(key) ? bundle.getString(key) : key;
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, locale()).format(arguments);
    }

    /** Compatibility for existing constant messages. Unknown text is returned verbatim. */
    public static String translate(String source) {
        String key = LEGACY_KEYS.get(source);
        if (key == null) {
            key = BUSINESS_DISPLAY_KEYS.get(source);
        }
        return key == null ? source : message(key);
    }

    public static String label(String chinese, String english) {
        if ("en".equals(locale().getLanguage())) {
            if (english != null && !english.isBlank()) {
                return english;
            }
            String key = BUSINESS_DISPLAY_KEYS.get(chinese);
            if (key != null) {
                return message(key);
            }
        }
        return translate(chinese);
    }

    /** Locale-independent display lookup used while generating bilingual artifacts. */
    public static String englishLabel(String source, String explicitEnglish) {
        if (explicitEnglish != null && !explicitEnglish.isBlank()) return explicitEnglish;
        String key = LEGACY_KEYS.get(source);
        return key != null && ENGLISH.containsKey(key) ? ENGLISH.getString(key) : source;
    }

    private static Map<String, String> businessDisplayKeys() {
        Map<String, String> keys = new HashMap<>();
        if (BUSINESS_CHINESE != null) {
            for (String key : BUSINESS_CHINESE.keySet()) {
                if (key.startsWith("business.display.")) {
                    keys.put(BUSINESS_CHINESE.getString(key), key);
                }
            }
        }
        return keys;
    }

    private static Map<String, String> legacyKeys() {
        Map<String, String> keys = new HashMap<>();
        for (String key : CHINESE.keySet()) {
            String value = CHINESE.getString(key);
            // translate returns the declared pattern verbatim. Existing String/StrUtil
            // formatters still own interpolation, so arguments are never translated.
            keys.put(value, key);
        }
        return keys;
    }

    private static ResourceBundle businessBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUSINESS_BASENAME, locale,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        } catch (MissingResourceException ignored) {
            // The core also runs in services with no business module on their classpath.
            return null;
        }
    }
}
