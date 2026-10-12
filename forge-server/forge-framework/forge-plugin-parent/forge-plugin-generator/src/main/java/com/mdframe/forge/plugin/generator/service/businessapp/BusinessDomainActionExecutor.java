package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionStepResultVO;

import java.util.Map;

/**
 * 可插拔领域动作执行器。
 */
public interface BusinessDomainActionExecutor {

    /**
     * 领域动作类型，例如 QUANTITY、FUND、CONTRACT_STATE。
     */
    String actionType();

    /**
     * 执行领域动作。
     *
     * @param context 动作执行上下文
     * @param config  已解析的领域动作配置
     * @return 步骤执行结果
     */
    BusinessActionStepResultVO execute(BusinessActionExecutionContext context, Map<String, Object> config);
}
