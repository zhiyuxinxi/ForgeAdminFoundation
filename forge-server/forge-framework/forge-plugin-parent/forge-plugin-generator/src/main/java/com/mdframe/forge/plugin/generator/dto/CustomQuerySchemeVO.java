package com.mdframe.forge.plugin.generator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义查询方案响应。
 */
@Data
public class CustomQuerySchemeVO {

    private Long id;

    private String configKey;

    private String schemeName;

    private List<CustomQueryConditionDTO> conditions;

    private List<String> fields;

    private String orderByColumn;

    private String isAsc;

    private String renderMode;

    private Integer isDefault;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
