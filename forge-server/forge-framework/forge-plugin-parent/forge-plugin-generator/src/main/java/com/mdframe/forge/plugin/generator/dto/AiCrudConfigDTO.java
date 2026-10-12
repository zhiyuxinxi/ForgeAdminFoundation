package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiCrudConfigDTO {

    private Long id;
    private String configKey;
    private String tableName;
    private String tableComment;
    private String appName;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String options;
    private String mode;
    private String buildMode;
    private String status;
    private String publishStatus;
    private String menuName;
    private Long menuParentId;
    private Integer menuSort;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
    /** 页面模板类型 */
    private String layoutType;
    /** 可视化数据模型协议 */
    private String modelSchema;
    /** 可视化页面搭建协议 */
    private String pageSchema;
    private Integer draftVersion;
    private Integer publishedVersion;
}
