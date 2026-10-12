package com.mdframe.forge.plugin.data.vo;

import lombok.Data;

import java.util.List;

@Data
public class DataBusinessAiContextVO {

    private Long businessId;

    private String businessCode;

    private String businessName;

    private String businessDesc;

    private String analysisGoal;

    private String metricDefinition;

    private String dimensionDefinition;

    private String usageGuide;

    private List<DataBusinessDatasetVO> datasets;
}
