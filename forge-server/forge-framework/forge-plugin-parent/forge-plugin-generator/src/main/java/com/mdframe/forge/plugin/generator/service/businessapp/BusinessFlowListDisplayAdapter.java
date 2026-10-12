package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务应用侧流程列表展示适配器。
 */
@Component
@RequiredArgsConstructor
public class BusinessFlowListDisplayAdapter implements FlowBusinessListDisplayAdapter {

    private final BusinessFlowService businessFlowService;

    @Override
    public void enrich(List<FlowBusinessListDisplayItem> items) {
        businessFlowService.enrichBusinessListDisplay(items);
    }
}
