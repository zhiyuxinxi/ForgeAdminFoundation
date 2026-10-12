package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程表单不可变发布版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_flow_form_version")
public class FlowFormVersion extends TenantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private Long formId;

    private String formKey;

    private String formName;

    private String formCategory;

    private String formType;

    private Integer version;

    private String formSchema;

    private String fieldRegistry;

    private String formConfig;

    private String defaultDataMode;

    private LocalDateTime publishTime;

    private Long publishBy;

    private String remark;
}
