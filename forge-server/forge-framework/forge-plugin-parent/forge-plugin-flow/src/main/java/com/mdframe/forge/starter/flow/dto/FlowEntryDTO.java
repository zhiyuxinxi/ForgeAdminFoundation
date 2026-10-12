package com.mdframe.forge.starter.flow.dto;

import com.mdframe.forge.starter.flow.entity.FlowEntryFieldMapping;
import lombok.Data;

import java.util.List;

/**
 * 流程入口保存请求。
 */
@Data
public class FlowEntryDTO {

    private Long id;

    private String entryCode;

    private String entryName;

    private String entryDesc;

    private String modelKey;

    private String formKey;

    private Long formVersionId;

    private String dataMode;

    private String objectCode;

    private String configKey;

    private String visibleScope;

    private String titleTemplate;

    private String businessKeyTemplate;

    private String submitStrategy;

    private Integer status;

    private Integer sort;

    private List<FlowEntryFieldMapping> fieldMappings;
}
