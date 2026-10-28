package com.mdframe.forge.plugin.task.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_business_task_definition")
public class TaskDefinitionRow {
    private Long id;
    private Long tenantId;
    private String code;
    private Integer definitionVersion;
    private String title;
    private String sourceType;
    private Long createBy;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
