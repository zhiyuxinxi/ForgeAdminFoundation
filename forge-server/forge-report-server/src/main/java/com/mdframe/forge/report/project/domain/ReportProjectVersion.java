package com.mdframe.forge.report.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 大屏项目发布版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_project_version")
public class ReportProjectVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 版本名称
     */
    private String versionName;

    /**
     * 操作类型：publish/rollback
     */
    private String operationType;

    /**
     * 回退来源版本ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long sourceVersionId;

    /**
     * 发布人ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long publisherId;

    /**
     * 发布人名称
     */
    private String publisherName;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 发布地址
     */
    private String publishUrl;

    /**
     * 封面图文件ID
     */
    private String indexImg;

    /**
     * 画布宽度
     */
    private Integer canvasWidth;

    /**
     * 画布高度
     */
    private Integer canvasHeight;

    /**
     * 背景颜色
     */
    private String backgroundColor;

    /**
     * 组件配置JSON
     */
    private String componentData;

    /**
     * 备注
     */
    private String remark;
}
