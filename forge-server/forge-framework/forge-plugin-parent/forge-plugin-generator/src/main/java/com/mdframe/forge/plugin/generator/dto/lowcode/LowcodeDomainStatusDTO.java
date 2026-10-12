package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码业务领域启停请求。
 */
@Data
public class LowcodeDomainStatusDTO {

    /** ENABLED-启用，DISABLED-停用 */
    private String status;
}
