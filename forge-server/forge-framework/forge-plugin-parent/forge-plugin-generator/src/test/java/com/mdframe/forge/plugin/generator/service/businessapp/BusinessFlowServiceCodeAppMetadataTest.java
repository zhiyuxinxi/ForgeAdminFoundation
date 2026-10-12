package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CODE应用metadata绑定保存")
class BusinessFlowServiceCodeAppMetadataTest {

    private BusinessBindingMapper bindingMapper;
    private BusinessFlowService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        bindingMapper = mock(BusinessBindingMapper.class);
        service = new BusinessFlowService(
                bindingMapper,
                mock(BusinessFlowInstanceLinkMapper.class),
                mock(AiCrudConfigMapper.class),
                mock(BusinessObjectMapper.class),
                mock(BusinessDocumentConfigService.class),
                mock(BusinessDocumentRuntimeService.class),
                mock(DynamicCrudService.class),
                mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class),
                mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("首次保存创建完整绑定，再次保存复用并保留流程配置")
    void firstSaveCreatesReadableBindingAndSecondSaveReusesIt() {
        String objectCode = "generic_code_object";
        AtomicReference<AiBusinessBinding> storedBinding = new AtomicReference<>();
        when(bindingMapper.selectBindingByTypeAndCode(7L, "OBJECT", objectCode, "FLOW"))
                .thenAnswer(invocation -> storedBinding.get());
        doAnswer(invocation -> {
            AiBusinessBinding inserted = invocation.getArgument(0);
            inserted.setId(1001L);
            storedBinding.set(inserted);
            return 1;
        }).when(bindingMapper).insert(any(AiBusinessBinding.class));

        Map<String, Object> firstMetadata = metadata("事件类型（设计验证）");
        assertTrue(service.saveCodeAppMetadata(objectCode, firstMetadata));

        AiBusinessBinding created = storedBinding.get();
        assertNotNull(created);
        assertEquals(7L, created.getTenantId());
        assertEquals("OBJECT", created.getTargetType());
        assertEquals(objectCode, created.getTargetCode());
        assertEquals("FLOW", created.getBindingType());
        assertEquals(objectCode, created.getBindingKey());
        assertEquals(objectCode + "业务表单资产配置", created.getBindingName());
        assertEquals(1, created.getStatus());
        assertEquals(0, created.getSortOrder());
        assertMetadataLabel(service.getCodeAppMetadata(objectCode), "事件类型（设计验证）");
        verify(bindingMapper, times(1)).insert(created);
        verify(bindingMapper, never()).updateById(any(AiBusinessBinding.class));

        created.setBindingKey("existing_flow_key");
        created.setBindingName("已有流程绑定");
        created.setStatus(0);
        JSONObject existingConfig = JSON.parseObject(created.getBindingConfig());
        existingConfig.put("flowModelKey", "existing_flow_key");
        existingConfig.put("variableMapping", List.of(Map.of("formField", "reporter", "flowVariable", "applicant")));
        existingConfig.getJSONObject("options").put("existingOption", "keep-me");
        created.setBindingConfig(existingConfig.toJSONString());

        Map<String, Object> secondMetadata = metadata("事件类型（再次保存）");
        assertTrue(service.saveCodeAppMetadata(objectCode, secondMetadata));

        assertSame(created, storedBinding.get());
        assertEquals("existing_flow_key", created.getBindingKey());
        assertEquals("已有流程绑定", created.getBindingName());
        assertEquals(0, created.getStatus());
        JSONObject updatedConfig = JSON.parseObject(created.getBindingConfig());
        assertEquals("existing_flow_key", updatedConfig.getString("flowModelKey"));
        assertEquals("keep-me", updatedConfig.getJSONObject("options").getString("existingOption"));
        assertEquals("reporter", updatedConfig.getJSONArray("variableMapping").getJSONObject(0).getString("formField"));
        assertMetadataLabel(service.getCodeAppMetadata(objectCode), "事件类型（再次保存）");
        verify(bindingMapper, times(1)).insert(created);
        verify(bindingMapper, times(1)).updateById(created);
    }

    @Test
    @DisplayName("provider显式查询能力覆盖旧metadata false并保留显式禁止")
    @SuppressWarnings("unchecked")
    void providerSearchCapabilityWinsOverLegacyNormalizedMetadata() throws Exception {
        List<Map<String, Object>> providerFields = List.of(
                Map.of("field", "status", "readonly", true, "searchable", true),
                Map.of("field", "title", "readonly", true, "searchable", true),
                Map.of("field", "reportNo", "readonly", true, "searchable", false),
                Map.of("field", "location", "readonly", false));
        List<Map<String, Object>> legacyMetadata = List.of(
                Map.of("field", "status", "label", "状态", "searchable", false),
                Map.of("field", "title", "label", "标题", "searchable", false),
                Map.of("field", "reportNo", "label", "上报编号", "searchable", true),
                Map.of("field", "location", "label", "发生地点", "searchable", false));

        List<Map<String, Object>> merged = CodeFormMetadata.mergeCodeAppFields(
                providerFields, legacyMetadata, Set.of());

        assertEquals(true, merged.get(0).get("searchable"));
        assertEquals(true, merged.get(1).get("searchable"));
        assertEquals(false, merged.get(2).get("searchable"));
        assertEquals(false, merged.get(3).get("searchable"));
        assertEquals("状态", merged.get(0).get("label"));
    }

    private Map<String, Object> metadata(String label) {
        return Map.of(
                "formAssets", List.of(Map.of(
                        "formKey", "generic_code_object_form",
                        "fields", List.of(Map.of("field", "eventType", "label", label)))),
                "viewSchema", Map.of("columns", List.of(Map.of("field", "eventType", "title", label))));
    }

    @SuppressWarnings("unchecked")
    private void assertMetadataLabel(Map<String, Object> metadata, String expectedLabel) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) metadata.get("formAssets");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) assets.get(0).get("fields");
        assertEquals(expectedLabel, fields.get(0).get("label"));
    }
}
