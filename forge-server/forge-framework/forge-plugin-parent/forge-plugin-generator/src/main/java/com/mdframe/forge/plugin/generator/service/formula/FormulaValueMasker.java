package com.mdframe.forge.plugin.generator.service.formula;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Masks sensitive values before formula runtime data is persisted or returned.
 */
@Component
public class FormulaValueMasker {

    private static final String MASK_VALUE = "****";

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
        "(?i)(\"(?:phone|mobile|telephone|idCard|id_card|identityNo|identity_no|bankCard|bank_card|cardNo|card_no|token|accessToken|access_token|apiKey|api_key|secret|password|ak|sk)\"\\s*:\\s*\")([^\"]*)(\")");

    private static final Pattern MOBILE_PATTERN =
        Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");

    // 18 位身份证：6 位地区 + 8 位出生日期 + 4 位校验/顺序；中间出生月日共 4 位
    private static final Pattern ID_CARD_PATTERN =
        Pattern.compile("(?<!\\d)([1-9]\\d{5}\\d{4})\\d{4}(\\d{3}[0-9Xx])(?!\\d)");

    private static final Pattern BANK_CARD_PATTERN =
        Pattern.compile("(?<!\\d)(\\d{6})\\d{6,9}(\\d{4})(?!\\d)");

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = SENSITIVE_KEY_PATTERN.matcher(value).replaceAll("$1" + MASK_VALUE + "$3");
        masked = MOBILE_PATTERN.matcher(masked).replaceAll("$1****$2");
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("$1******$2");
        return BANK_CARD_PATTERN.matcher(masked).replaceAll("$1****$2");
    }
}
