package com.mdframe.forge.starter.core.util;

import java.util.Locale;
import java.util.Set;

/**
 * 敏感数据脱敏工具。
 */
public final class SensitiveDataUtil {

    public static final String MASK = "******";

    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "pwd", "secret", "token", "credential",
            "accesskey", "secretkey", "privatekey", "apikey", "appsecret",
            "accesssecret", "clientsecret"
    );

    private SensitiveDataUtil() {
    }

    public static boolean isMaskedValue(String value) {
        return value != null && MASK.equals(value.trim());
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(".", "");
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static String maskSensitiveValue(String key, String value) {
        if (value == null) {
            return null;
        }
        return isSensitiveKey(key) ? MASK : value;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return maskMiddle(value, 1, 1);
        }
        return maskMiddle(value, 3, 4);
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        String value = idCard.trim();
        if (value.length() <= 8) {
            return maskMiddle(value, 2, 2);
        }
        return maskMiddle(value, 4, 4);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskMiddle(email, 1, 1);
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + MASK + domain;
        }
        return local.charAt(0) + MASK + local.charAt(local.length() - 1) + domain;
    }

    public static String maskMiddle(String value, int prefixLength, int suffixLength) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int length = value.length();
        if (length <= prefixLength + suffixLength) {
            return MASK;
        }
        String prefix = value.substring(0, Math.max(0, prefixLength));
        String suffix = value.substring(length - Math.max(0, suffixLength));
        return prefix + MASK + suffix;
    }
}
