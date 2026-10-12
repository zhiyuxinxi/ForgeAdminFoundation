package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

@Data
public class DataBusinessDatasetDTO {

    private Long id;

    private Long datasetId;

    private Integer isPrimary;

    private Integer sort;

    private String usageRemark;
}
