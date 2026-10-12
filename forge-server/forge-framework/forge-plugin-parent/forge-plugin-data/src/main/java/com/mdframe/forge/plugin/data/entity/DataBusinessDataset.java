package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_business_dataset")
public class DataBusinessDataset extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long businessId;

    private Long datasetId;

    private Integer isPrimary;

    private Integer sort;

    private String usageRemark;

    @TableField(exist = false)
    private String datasetCode;

    @TableField(exist = false)
    private String datasetName;

    @TableField(exist = false)
    private String datasetType;

    @TableField(exist = false)
    private String datasetDescription;

    @TableField(exist = false)
    private String paramSchemaJson;
}
