package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;

import java.util.List;
import java.util.Map;

/**
 * @deprecated Implement the lowcode-api provider in business modules. This bridge keeps
 * existing providers usable during their explicit Harness migration; it owns no policy.
 */
@Deprecated
public interface BusinessCodeFormProvider extends com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider {

    BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                                           Map<String, Object> formRef,
                                           List<Map<String, Object>> fieldPermissions);

    default BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                                                  Map<String, Object> formRef,
                                                  List<Map<String, Object>> fieldPermissions) {
        return CodeFormProviderCompatibility.context(
                com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider.super.saveContext(
                        dto, formRef, fieldPermissions));
    }

    @Override
    default com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextVO buildContext(
            com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextQueryDTO query,
            Map<String, Object> formRef, List<Map<String, Object>> fieldPermissions) {
        return buildContext(CodeFormProviderCompatibility.query(query), formRef, fieldPermissions);
    }

    @Override
    default com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextVO saveContext(
            com.mdframe.forge.lowcode.api.form.BusinessTaskFormSaveDTO dto,
            Map<String, Object> formRef, List<Map<String, Object>> fieldPermissions) {
        return saveContext(CodeFormProviderCompatibility.save(dto), formRef, fieldPermissions);
    }
}
