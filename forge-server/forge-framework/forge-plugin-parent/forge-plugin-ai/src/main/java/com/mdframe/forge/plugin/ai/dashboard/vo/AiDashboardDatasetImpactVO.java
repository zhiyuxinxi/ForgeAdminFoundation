package com.mdframe.forge.plugin.ai.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集影响的大屏组件。
 */
@Data
public class AiDashboardDatasetImpactVO {

    private Long lineageId;

    private Long recordId;

    private Long projectId;

    private String projectName;

    private Long businessDefinitionId;

    private String businessName;

    private Integer componentIndex;

    private String componentKey;

    private String componentTitle;

    private Long datasetId;

    private String datasetName;

    private String fieldNames;

    private String bindingStatus;

    private String generatedTitle;

    private String recordStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
