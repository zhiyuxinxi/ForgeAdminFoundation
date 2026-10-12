package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码模型内的业务对象。
 */
@Data
public class LowcodeObjectSchema {

    private String code;

    private String name;

    private String description;
}
