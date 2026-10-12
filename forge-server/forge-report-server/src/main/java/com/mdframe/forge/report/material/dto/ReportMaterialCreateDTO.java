package com.mdframe.forge.report.material.dto;

import lombok.Data;

/**
 * 报表素材入库参数。
 */
@Data
public class ReportMaterialCreateDTO {

    /**
     * 通用文件ID。
     */
    private String fileId;

    /**
     * 素材分类。
     */
    private String materialCategory;

    /**
     * 兼容前端旧字段，等同 materialCategory。
     */
    private String businessId;

    /**
     * 是否私有。
     */
    private Boolean isPrivate;
}
