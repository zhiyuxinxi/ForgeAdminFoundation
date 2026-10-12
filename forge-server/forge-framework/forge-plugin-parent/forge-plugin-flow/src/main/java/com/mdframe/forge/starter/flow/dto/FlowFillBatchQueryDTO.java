package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/**
 * 组织填报批次查询条件。
 */
@Data
public class FlowFillBatchQueryDTO {

    private String batchName;

    private String entryCode;

    private String periodKey;

    private String status;
}
