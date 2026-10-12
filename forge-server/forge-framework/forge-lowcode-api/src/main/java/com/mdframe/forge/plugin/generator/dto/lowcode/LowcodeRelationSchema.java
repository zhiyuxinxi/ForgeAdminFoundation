package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码领域内业务对象关系协议。
 */
@Data
public class LowcodeRelationSchema {

    private String relationType;

    private String targetObjectCode;

    private String sourceField;

    private String targetField;

    private String displayField;
}
