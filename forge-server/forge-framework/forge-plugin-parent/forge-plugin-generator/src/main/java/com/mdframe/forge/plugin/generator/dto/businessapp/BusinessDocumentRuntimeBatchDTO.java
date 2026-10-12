package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务单据运行态批量查询请求。
 */
@Data
public class BusinessDocumentRuntimeBatchDTO {

    private List<Long> recordIds = new ArrayList<>();
}
