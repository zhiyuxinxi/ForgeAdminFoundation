package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

/**
 * 自定义查询单个条件。
 */
@Data
public class CustomQueryConditionDTO {

    /**
     * 连接关系：AND / OR。
     */
    private String relation;

    /**
     * 字段名，使用前端驼峰字段。
     */
    private String field;

    /**
     * 操作符：eq/ne/like/gt/ge/lt/le/in/between/is_null/is_not_null。
     */
    private String operator;

    /**
     * 查询值。
     */
    private Object value;

    /**
     * 区间结束值。
     */
    private Object valueEnd;

    /**
     * 树形字段是否包含子级节点。
     */
    private Boolean includeChildren;
}
