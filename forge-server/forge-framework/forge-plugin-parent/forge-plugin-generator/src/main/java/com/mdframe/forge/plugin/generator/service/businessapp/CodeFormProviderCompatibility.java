package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Temporary adaptation at the generator boundary; runtime contracts own all fields. */
final class CodeFormProviderCompatibility {
    private CodeFormProviderCompatibility() {
    }

    static BusinessCodeFormProvider legacy(com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider provider) {
        return provider instanceof BusinessCodeFormProvider legacy ? legacy : new RuntimeProviderAdapter(provider);
    }

    static BusinessTaskFormContextQueryDTO query(
            com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextQueryDTO source) {
        return source instanceof BusinessTaskFormContextQueryDTO legacy
                ? legacy : copy(source, new BusinessTaskFormContextQueryDTO());
    }

    static BusinessTaskFormSaveDTO save(com.mdframe.forge.lowcode.api.form.BusinessTaskFormSaveDTO source) {
        return source instanceof BusinessTaskFormSaveDTO legacy
                ? legacy : copy(source, new BusinessTaskFormSaveDTO());
    }

    static BusinessTaskFormContextVO context(com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextVO source) {
        return source instanceof BusinessTaskFormContextVO legacy
                ? legacy : copy(source, new BusinessTaskFormContextVO());
    }

    private static <T> T copy(Object source, T target) {
        if (source == null) {
            return null;
        }
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private record RuntimeProviderAdapter(com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider delegate)
            implements BusinessCodeFormProvider {
        @Override
        public String providerKey() {
            return delegate.providerKey();
        }

        @Override
        public String providerName() {
            return delegate.providerName();
        }

        @Override
        public List<Map<String, Object>> formAssets(String objectCode) {
            return delegate.formAssets(objectCode);
        }

        @Override
        public String buildSummary(Map<String, Object> recordData) {
            return delegate.buildSummary(recordData);
        }

        @Override
        public Map<Long, String> buildSummaries(String objectCode, Collection<Long> recordIds) {
            return delegate.buildSummaries(objectCode, recordIds);
        }

        @Override
        public BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                Map<String, Object> formRef, List<Map<String, Object>> fieldPermissions) {
            return context(delegate.buildContext(query, formRef, fieldPermissions));
        }

        @Override
        public BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                Map<String, Object> formRef, List<Map<String, Object>> fieldPermissions) {
            return context(delegate.saveContext(dto, formRef, fieldPermissions));
        }
    }
}
