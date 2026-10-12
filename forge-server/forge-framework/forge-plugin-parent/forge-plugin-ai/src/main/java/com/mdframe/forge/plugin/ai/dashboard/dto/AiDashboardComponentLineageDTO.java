package com.mdframe.forge.plugin.ai.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 大屏组件血缘保存项。
 */
@Data
public class AiDashboardComponentLineageDTO {

    private Integer componentIndex;

    private String componentKey;

    private String componentTitle;

    private Long datasetId;

    private String datasetName;

    private List<String> fieldNames;

    private String bindingStatus;
}
