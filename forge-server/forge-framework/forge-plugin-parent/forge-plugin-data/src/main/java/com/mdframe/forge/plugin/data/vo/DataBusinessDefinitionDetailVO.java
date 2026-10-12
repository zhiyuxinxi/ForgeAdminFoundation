package com.mdframe.forge.plugin.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DataBusinessDefinitionDetailVO {

    private Long id;

    private String businessCode;

    private String businessName;

    private String businessDesc;

    private String analysisGoal;

    private String metricDefinition;

    private String dimensionDefinition;

    private String usageGuide;

    private Integer status;

    private Integer datasetCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DataBusinessDatasetVO> datasets;
}
