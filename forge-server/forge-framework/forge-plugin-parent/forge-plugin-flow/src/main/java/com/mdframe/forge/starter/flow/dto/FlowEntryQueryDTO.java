package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/**
 * 流程入口查询条件。
 */
@Data
public class FlowEntryQueryDTO {

    private String entryName;

    private String entryCode;

    private String modelKey;

    private String formKey;

    private String dataMode;

    private Integer status;
}
