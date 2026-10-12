package com.mdframe.forge.lowcode.api.form;

import lombok.Data;

/**
 * 业务待办表单上下文查询参数。
 */
@Data
public class BusinessTaskFormContextQueryDTO {

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String objectCode;

    private Long recordId;

    private String formKey;
}
