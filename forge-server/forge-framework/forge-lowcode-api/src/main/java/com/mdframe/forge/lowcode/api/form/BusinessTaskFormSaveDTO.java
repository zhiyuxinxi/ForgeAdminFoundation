package com.mdframe.forge.lowcode.api.form;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务待办表单保存参数。
 */
@Data
public class BusinessTaskFormSaveDTO {

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String objectCode;

    private Long recordId;

    private String formKey;

    private Map<String, Object> data = new LinkedHashMap<>();
}
