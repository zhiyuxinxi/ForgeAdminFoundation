package com.mdframe.forge.starter.log.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;

final class AuditEventText {
    static String truncate(String str, int maxLength) {
        if (StrUtil.isBlank(str) || maxLength <= 0) {
            return str;
        }
        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...";
        }
        return str;
    }

    static String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return JSONObject.toJSONString(value);
    }
}
