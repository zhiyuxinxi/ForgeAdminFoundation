package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.lowcode.api.form.BusinessTaskFormContextVO;
import com.mdframe.forge.lowcode.api.form.BusinessTaskFormSaveDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LowcodeRuntimeFormContractTest {
    private static final Map<String, Object> FORM = Map.of("formKey", "review");
    private static final List<Map<String, Object>> PERMISSIONS = List.of(Map.of("field", "amount", "readOnly", true));

    @Test
    void registryFindsAnApiOnlyProviderAndPreservesRuntimeFields() {
        var provider = new ApiProvider();
        var registry = registry(provider);
        var query = new com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO();
        query.setTaskId("task-7");
        query.setRecordId(41L);
        var context = registry.require("api").buildContext(query, FORM, PERMISSIONS);
        assertEquals("task-7", context.getTaskId());
        assertEquals(41L, context.getRecordId());
        assertEquals("page-3", context.getPageId());
        assertFalse(context.getAllowApprove());
        assertTrue(context.getRequireSignature());
        assertEquals(List.of("notice"), context.getWarnings());
        assertSame(FORM, context.getFormRef());
        assertSame(PERMISSIONS, context.getFieldPermissions());
        assertEquals(List.of(Map.of("field", "amount")), context.getFields());
        assertEquals("API", registry.listProviderCatalog("invoice").get(0).get("providerName"));
        assertEquals("api", registry.listAssets("invoice").get(0).get("providerKey"));
    }

    @Test
    void registryPreservesApiSavePayloadAndBusinessFailures() {
        var provider = new ApiProvider();
        var legacy = registry(provider).require("api");
        var save = new com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO();
        save.setData(Map.of("amount", 9));
        var result = legacy.saveContext(save, FORM, PERMISSIONS);
        assertSame(save.getData(), result.getRecordData());
        assertSame(PERMISSIONS, result.getFieldPermissions());
        provider.failure = new BusinessException("denied");
        assertSame(provider.failure, assertThrows(BusinessException.class,
                () -> legacy.buildContext(null, FORM, PERMISSIONS)));
    }

    @Test
    void existingProviderRemainsTheSameBeanAndAcceptsTheNewApiRequest() {
        var old = new LegacyProvider();
        assertSame(old, registry(old).require("legacy"));
        com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider runtime = old;
        var query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId("task");
        query.setBusinessKey("invoice:41");
        query.setProcessInstanceId("process");
        query.setProcessDefKey("definition");
        query.setTaskDefKey("review");
        query.setObjectCode("invoice");
        query.setRecordId(41L);
        query.setFormKey("edit");
        var result = runtime.buildContext(query, FORM, PERMISSIONS);
        assertEquals(query.getTaskId(), old.query.getTaskId());
        assertEquals(query.getBusinessKey(), old.query.getBusinessKey());
        assertEquals(query.getProcessInstanceId(), old.query.getProcessInstanceId());
        assertEquals(query.getProcessDefKey(), old.query.getProcessDefKey());
        assertEquals(query.getTaskDefKey(), old.query.getTaskDefKey());
        assertEquals(query.getObjectCode(), old.query.getObjectCode());
        assertEquals(query.getRecordId(), old.query.getRecordId());
        assertEquals(query.getFormKey(), old.query.getFormKey());
        assertSame(old.result, result);
        var save = new BusinessTaskFormSaveDTO();
        save.setTaskId("task");
        save.setData(Map.of("amount", 3));
        runtime.saveContext(save, FORM, PERMISSIONS);
        assertEquals("task", old.save.getTaskId());
        assertSame(save.getData(), old.save.getData());
    }

    @Test
    void missingProviderAndUnsupportedSaveKeepTheirFailureSemantics() {
        var provider = new com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider() {
            public String providerKey() { return "readonly"; }
            public BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                    Map<String, Object> formRef, List<Map<String, Object>> permissions) { return null; }
        };
        var registry = registry(provider);
        assertTrue(registry.find(" ").isEmpty());
        assertThrows(BusinessException.class, () -> registry.require("unknown"));
        assertNull(registry.require("readonly").buildContext(null, FORM, PERMISSIONS));
        assertThrows(BusinessException.class, () -> registry.require("readonly")
                .saveContext(new com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO(),
                        FORM, PERMISSIONS));
    }

    private static BusinessCodeFormProviderRegistry registry(
            com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider provider) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton(provider.providerKey(), provider);
        return new BusinessCodeFormProviderRegistry(
                beans.getBeanProvider(com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider.class));
    }

    private static final class ApiProvider implements com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider {
        private BusinessException failure;
        public String providerKey() { return "api"; }
        public String providerName() { return "API"; }
        public List<Map<String, Object>> formAssets(String objectCode) {
            return List.of(Map.of("formKey", "edit", "fields", List.of(Map.of("field", "amount"))));
        }
        public BusinessTaskFormContextVO buildContext(BusinessTaskFormContextQueryDTO query,
                Map<String, Object> formRef, List<Map<String, Object>> permissions) {
            if (failure != null) { throw failure; }
            var result = new BusinessTaskFormContextVO();
            result.setTaskId(query.getTaskId());
            result.setRecordId(query.getRecordId());
            result.setPageId("page-3");
            result.setAllowApprove(false);
            result.setRequireSignature(true);
            result.setWarnings(List.of("notice"));
            result.setFormRef(formRef);
            result.setFieldPermissions(permissions);
            result.setFields(List.of(Map.of("field", "amount")));
            return result;
        }
        public BusinessTaskFormContextVO saveContext(BusinessTaskFormSaveDTO dto,
                Map<String, Object> formRef, List<Map<String, Object>> permissions) {
            var result = new BusinessTaskFormContextVO();
            result.setRecordData(dto.getData());
            result.setFieldPermissions(permissions);
            return result;
        }
    }

    private static final class LegacyProvider implements BusinessCodeFormProvider {
        private com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO query;
        private com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO save;
        private final com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO result =
                new com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO();
        public String providerKey() { return "legacy"; }
        public com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO buildContext(
                com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO value,
                Map<String, Object> formRef, List<Map<String, Object>> permissions) {
            query = value;
            return result;
        }
        public com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO saveContext(
                com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO value,
                Map<String, Object> formRef, List<Map<String, Object>> permissions) {
            save = value;
            return result;
        }
    }
}
