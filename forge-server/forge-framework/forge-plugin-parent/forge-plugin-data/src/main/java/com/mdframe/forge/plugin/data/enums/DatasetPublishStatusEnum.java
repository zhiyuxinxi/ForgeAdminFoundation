package com.mdframe.forge.plugin.data.enums;

import java.util.Objects;

public enum DatasetPublishStatusEnum {

    DRAFT(0, "未发布"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下架");

    private final Integer code;

    private final String description;

    DatasetPublishStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isPublished(Integer code) {
        return Objects.equals(PUBLISHED.code, code);
    }
}
