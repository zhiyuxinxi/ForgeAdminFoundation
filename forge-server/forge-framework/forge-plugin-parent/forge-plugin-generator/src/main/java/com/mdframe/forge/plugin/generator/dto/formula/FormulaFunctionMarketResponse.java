package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公式函数市场响应。
 */
@Data
public class FormulaFunctionMarketResponse {

    private Long id;

    private Long tenantId;

    private String functionCode;

    private String displayName;

    private String category;

    private String description;

    private String sourceType;

    private String argumentSchema;

    private String returnType;

    private String example;

    private String implementationType;

    private String beanName;

    private String methodName;

    private String releaseNote;

    private String status;

    private String currentVersion;

    private String latestVersion;

    private Boolean builtin;

    private Integer sortOrder;

    private String installedVersion;

    private String installStatus;

    private Boolean enabled;

    private Long installedBy;

    private LocalDateTime installedTime;
}
