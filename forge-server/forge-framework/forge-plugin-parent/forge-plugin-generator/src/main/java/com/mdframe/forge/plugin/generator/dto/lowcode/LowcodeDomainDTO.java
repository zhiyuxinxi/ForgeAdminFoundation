package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码业务领域保存请求。
 */
@Data
public class LowcodeDomainDTO {

    private Long id;

    private Long parentId;

    private String domainCode;

    private String domainName;

    private String domainDesc;

    private String icon;

    private Integer sort;

    private String status;

    private Long menuParentId;

    private String tablePrefix;

    private String configKeyPrefix;

    private String defaultAppType;

    private String defaultLayoutType;

    private String defaultTableMode;

    private LowcodeDomainSchema domainSchema;
}
