package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.entity.FlowEntryFieldMapping;
import com.mdframe.forge.starter.flow.entity.FlowFormVersion;
import lombok.Data;

import java.util.List;

/**
 * 流程入口运行配置。
 */
@Data
public class FlowEntryRuntimeVO {

    private FlowEntry entry;

    private FlowFormVersion formVersion;

    private String formSchema;

    private String fieldRegistry;

    private List<FlowEntryFieldMapping> fieldMappings;
}
