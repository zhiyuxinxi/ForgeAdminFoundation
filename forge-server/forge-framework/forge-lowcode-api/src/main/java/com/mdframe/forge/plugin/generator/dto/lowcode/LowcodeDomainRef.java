package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码模型内的业务领域引用。
 */
@Data
public class LowcodeDomainRef {

    private Long id;

    private String code;

    private String name;
}
