package com.mdframe.forge.flow.client.spi;

import java.util.List;

/**
 * 流程列表业务展示扩展点。
 *
 * <p>流程插件只知道流程任务和抄送记录，业务对象名称、业务摘要由业务应用侧按
 * businessKey 批量补齐，避免流程插件反向依赖 generator。</p>
 */
public interface FlowBusinessListDisplayAdapter {

    /**
     * 批量补齐流程列表项的业务对象名和业务摘要。
     */
    void enrich(List<FlowBusinessListDisplayItem> items);
}
