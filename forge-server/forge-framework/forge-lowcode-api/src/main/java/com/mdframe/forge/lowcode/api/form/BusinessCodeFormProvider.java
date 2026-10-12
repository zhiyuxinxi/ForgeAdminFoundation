package com.mdframe.forge.lowcode.api.form;

import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 代码优先复杂业务的流程表单提供方。
 * <p>
 * 业务模块实现该接口并注册为 Spring Bean 后，流程配置可通过 providerKey/formKey
 * 引用代码业务表单，平台待办页通过统一上下文协议接入。
 */
public interface BusinessCodeFormProvider {

    /**
     * Provider 唯一编码。
     */
    String providerKey();

    /**
     * Provider 展示名称。
     */
    default String providerName() {
        return providerKey();
    }

    /**
     * 当前 Provider 可提供的表单资产。
     */
    default List<Map<String, Object>> formAssets(String objectCode) {
        return List.of();
    }

    /**
     * 根据业务记录构建用户可读摘要。默认不提供，由具体 Provider 按业务字段实现。
     */
    default String buildSummary(Map<String, Object> recordData) {
        return null;
    }

    /**
     * 根据业务记录 ID 批量构建用户可读摘要。列表页应优先实现该方法，避免逐条查询。
     */
    default Map<Long, String> buildSummaries(String objectCode, Collection<Long> recordIds) {
        return Map.of();
    }

    /**
     * 构建待办页代码业务表单上下文。
     */
    BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> formRef,
                                           List<Map<String, Object>> fieldPermissions);

    /**
     * 保存待办页允许编辑的代码业务表单数据。
     */
    default BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                                                  Map<String, Object> formRef,
                                                  List<Map<String, Object>> fieldPermissions) {
        throw new BusinessException("当前代码表单未实现待办保存能力");
    }
}
