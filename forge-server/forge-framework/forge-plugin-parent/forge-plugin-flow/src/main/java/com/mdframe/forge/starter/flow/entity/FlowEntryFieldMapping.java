package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程入口字段映射。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_flow_entry_field_mapping")
public class FlowEntryFieldMapping extends TenantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private Long entryId;

    private String formField;

    private String targetType;

    private String targetField;

    private String flowVariable;

    private Integer required;

    private String mappingConfig;

    private Integer sort;

    @TableLogic
    private Integer deleted;
}
