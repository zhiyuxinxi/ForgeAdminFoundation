package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataBusinessDefinitionSaveDTO {

    private Long id;

    private String businessCode;

    private String businessName;

    private String businessDesc;

    private String analysisGoal;

    private String metricDefinition;

    private String dimensionDefinition;

    private String usageGuide;

    private Integer status;

    private List<DataBusinessDatasetDTO> datasets;
}
