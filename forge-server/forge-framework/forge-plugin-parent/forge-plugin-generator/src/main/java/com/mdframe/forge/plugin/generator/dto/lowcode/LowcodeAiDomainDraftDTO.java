package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 规划出的业务领域草稿。existingDomainId 为空时表示需要用户确认后新建领域。
 */
@Data
public class LowcodeAiDomainDraftDTO {

    private Long existingDomainId;

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

    private List<String> objectCodes = new ArrayList<>();
}
