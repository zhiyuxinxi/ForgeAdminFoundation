package com.mdframe.forge.plugin.generator.vo.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainSchema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 低代码业务领域详情。
 */
@Data
public class LowcodeDomainVO {

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

    private Long appCount;

    private Long publishedCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
