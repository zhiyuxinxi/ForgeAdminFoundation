package com.mdframe.forge.plugin.generator.vo.lowcode;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 低代码应用详情。
 */
@Data
public class LowcodeAppDetailVO {

    private Long id;

    private String configKey;

    private String tableName;

    private String tableComment;

    private String appName;

    private Long domainId;

    private String domainCode;

    private String domainName;

    private String objectCode;

    private String objectName;

    private String mode;

    private String buildMode;

    private String status;

    private String publishStatus;

    private String menuName;

    private Long menuParentId;

    private Integer menuSort;

    private Long menuResourceId;

    private String layoutType;

    private Object modelSchema;

    private Object pageSchema;

    private Integer draftVersion;

    private Integer publishedVersion;

    private LocalDateTime publishTime;

    private Long publishBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
