package com.mdframe.forge.plugin.data.vo;

import lombok.Data;

import java.util.List;

@Data
public class DataBusinessDatasetVO {

    private Long id;

    private Long datasetId;

    private String datasetCode;

    private String datasetName;

    private String datasetType;

    private String description;

    private String paramSchemaJson;

    private Integer isPrimary;

    private Integer sort;

    private String usageRemark;

    private List<DataDatasetFieldVO> fields;
}
