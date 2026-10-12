package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码应用迁移业务领域请求。
 */
@Data
public class LowcodeMoveDomainDTO {

    private Long domainId;

    private String objectCode;

    private String objectName;
}
