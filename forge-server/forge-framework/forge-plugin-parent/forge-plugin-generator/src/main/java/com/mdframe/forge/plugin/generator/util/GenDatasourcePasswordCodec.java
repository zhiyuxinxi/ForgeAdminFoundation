package com.mdframe.forge.plugin.generator.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 代码生成数据源密码加解密工具。
 */
@Slf4j
public final class GenDatasourcePasswordCodec {

    private static final String AES_KEY = "ForgeGenerator16";

    private GenDatasourcePasswordCodec() {
    }

    public static String encrypt(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        try {
            AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
            return aes.encryptHex(password);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败");
        }
    }

    public static String decrypt(String encryptedPassword) {
        if (StrUtil.isBlank(encryptedPassword)) {
            return encryptedPassword;
        }
        try {
            AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(encryptedPassword);
        } catch (Exception e) {
            log.warn("密码解密失败，可能是未加密的旧数据: {}", e.getMessage());
            return encryptedPassword;
        }
    }
}
