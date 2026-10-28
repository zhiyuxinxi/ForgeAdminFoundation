package com.mdframe.forge.plugin.system.util;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用户手机号校验负例：空值允许（与既有行为一致），非空必须符合项目手机号规则
 * （{@code ^1[3-9]\d{9}$}，与 PC 端 validation-presets.js 的 PHONE 预设一致）。
 */
class UserPhoneValidatorTest {

    @Test
    void acceptsValidChinaMobile() {
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone("13800138000")).doesNotThrowAnyException();
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone("19912345678")).doesNotThrowAnyException();
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone("15000000000")).doesNotThrowAnyException();
    }

    @Test
    void trimsWhitespaceAroundValidPhone() {
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone(" 13800138000 ")).doesNotThrowAnyException();
    }

    @Test
    void allowsEmptyPhone() {
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone(null)).doesNotThrowAnyException();
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone("")).doesNotThrowAnyException();
        assertThatCode(() -> UserPhoneValidator.validateOptionalPhone("   ")).doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortPhone() {
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
    }

    @Test
    void rejectsTooLongPhone() {
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("138001380001"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
    }

    @Test
    void rejectsInvalidPrefix() {
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("23800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("03800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
    }

    @Test
    void rejectsNonNumericPhone() {
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("abc"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
        assertThatThrownBy(() -> UserPhoneValidator.validateOptionalPhone("13800abc000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserPhoneValidator.PHONE_ERROR_MESSAGE);
    }
}
