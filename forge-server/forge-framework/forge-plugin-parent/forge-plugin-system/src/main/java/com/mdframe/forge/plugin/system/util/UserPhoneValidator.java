package com.mdframe.forge.plugin.system.util;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;

/**
 * 用户资料字段校验（PC / H5 共享同一规则，服务端兜底）。
 *
 * <p>手机号规则与 PC 端 {@code forge-admin-ui/src/utils/validation-presets.js}
 * 的 {@code PHONE} 预设一致：{@code ^1[3-9]\d{9}$}（11 位国内手机号）。</p>
 *
 * <p>语义：空值（null / 空白）允许（与既有行为一致），非空必须符合项目手机号规则；
 * 非法值抛出 {@link BusinessException}，由全局异常处理器返回
 * {@code RespInfo.error(500, "请输入正确的手机号")}。</p>
 */
public final class UserPhoneValidator {

    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    public static final String PHONE_ERROR_MESSAGE = "请输入正确的手机号";

    private UserPhoneValidator() {
    }

    /**
     * 校验可选手机号：空值允许，非空必须符合项目手机号规则。
     *
     * @param phone 手机号（可为 null / 空白）
     * @throws BusinessException 非空但格式非法时抛出
     */
    public static void validateOptionalPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        if (!phone.trim().matches(PHONE_PATTERN)) {
            throw new BusinessException(UiMessages.translate(PHONE_ERROR_MESSAGE));
        }
    }
}
