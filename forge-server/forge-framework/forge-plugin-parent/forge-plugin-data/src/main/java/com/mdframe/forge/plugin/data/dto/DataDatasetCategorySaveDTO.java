package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

@Data
public class DataDatasetCategorySaveDTO {

    private Long id;

    private Long parentId;

    private String categoryCode;

    private String categoryName;

    private Integer sortOrder;

    private Integer status;

    private String description;
}
