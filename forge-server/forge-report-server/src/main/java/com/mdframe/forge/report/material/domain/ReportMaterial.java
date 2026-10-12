package com.mdframe.forge.report.material.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报表素材实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_material")
public class ReportMaterial extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 通用文件ID。
     */
    private String fileId;

    /**
     * 素材分类：background/panel/icon/illustration。
     */
    private String materialCategory;

    /**
     * 是否私有：true 私有，false 公共。
     */
    private Boolean isPrivate;

    /**
     * 状态：1 正常，0 删除。
     */
    private Integer status;
}
