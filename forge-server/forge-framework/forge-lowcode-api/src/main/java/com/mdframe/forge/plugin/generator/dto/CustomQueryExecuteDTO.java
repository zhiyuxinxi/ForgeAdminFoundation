package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

import java.util.List;

/**
 * 自定义查询执行请求。
 */
@Data
public class CustomQueryExecuteDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 结果展示字段，使用前端驼峰字段。
     */
    private List<String> fields;

    private List<CustomQueryConditionDTO> conditions;

    private String orderByColumn;

    private String isAsc;

    /**
     * table / card。
     */
    private String renderMode;
}
