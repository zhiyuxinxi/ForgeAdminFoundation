package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessTaskDetailChildrenTest {
    private final FlowClient flow = mock(FlowClient.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final Map<String, Object> record = new LinkedHashMap<>();
    private BusinessFlowService service;
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(7L);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getTenantId).thenReturn(7L);
        session.when(SessionHelper::getUserId).thenReturn(42L);
        service = new BusinessFlowService(bindings, mock(BusinessFlowInstanceLinkMapper.class), configs, objects,
                mock(BusinessDocumentConfigService.class), mock(BusinessDocumentRuntimeService.class), data,
                mock(BusinessFieldDesignService.class), mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class), mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "flowClient", flow);
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of(
                "status", 1, "assignee", "42", "taskDefKey", "review")));
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setStatus(1);
        binding.setBindingConfig("""
                {"flowModelKey":"approval","nodeForms":[{"taskDefKey":"review",
                "formMode":"BUSINESS_OBJECT_FORM","formKey":"order"}]}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(binding);
        config.setConfigKey("order-config");
        config.setObjectCode("order");
        when(configs.selectPublishedByObjectCodeOrConfigKey(eq(7L), anyString())).thenReturn(config);
        when(data.getRuntimeValues("order-config")).thenAnswer(invocation -> {
            assertEquals(7L, TenantContextHolder.getTenantId());
            return RuntimeCrudConfigAdapter.fromDesign(config);
        });
        BusinessObjectVO object = new BusinessObjectVO();
        object.setObjectCode("order");
        object.setObjectName("Order");
        object.setConfigKey("order-config");
        object.setDesignerOptions("""
                {"formDesignerSchema":{"formKey":"order","fields":[{"field":"name","label":"Name"}]}}
                """);
        when(objects.selectObjectList(eq(7L), any())).thenReturn(List.of(object));
        record.put("name", "Example");
        when(data.selectById("order-config", 19L)).thenReturn(record);
    }

    @AfterEach
    void closeSession() {
        session.close();
        TenantContextHolder.clear();
    }

    @Test
    void detailChildrenKeepDeclaredOrderAndLegacyKeyFallbacks() {
        configure(List.of(child("key", "named"), child("modelCode", "model"),
                child("tableName", "table"), child("unused", "fallback")));
        List<Map<String, Object>> rows = List.of(Map.of("id", 1));
        Map<String, Object> originalChildren = Map.of(
                "named", rows, "model", rows, "table", rows, "children", rows, "unlisted", rows);
        record.put("children", originalChildren);
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertTrue(result.getConfigured());
        Map<?, ?> visible = (Map<?, ?>) result.getRecordData().get("children");
        assertEquals(List.of("named", "model", "table", "children"), new ArrayList<>(visible.keySet()));
        assertEquals(4, result.getChildrenConfig().size());
        assertSame(rows, visible.get("named"));
        assertSame(originalChildren, record.get("children"));
        var reads = inOrder(data);
        reads.verify(data).selectById("order-config", 19L);
        reads.verify(data, times(3)).getRuntimeValues("order-config");
    }

    @Test
    void hiddenEmptyAndReferenceRelationsAreExcludedFromConfigAndData() {
        List<Map<String, Object>> children = new ArrayList<>();
        Map<String, Object> hidden = child("key", "hidden");
        hidden.put("showInDetail", false);
        children.add(hidden);
        children.add(Map.of("key", "empty", "fields", List.of()));
        for (String relation : List.of("reference", "LOOKUP", "OBJECT_REFERENCE", "OBJECTREFERENCE", "MANY_TO_ONE", "ONE_TO_ONE")) {
            Map<String, Object> reference = child("key", relation);
            reference.put("relationType", relation);
            children.add(reference);
        }
        children.add(child("key", "allowed"));
        children.add(child("key", "notList"));
        configure(children);
        record.put("children", Map.of("hidden", List.of(Map.of("id", 1)),
                "allowed", List.of(Map.of("id", 2)), "notList", "invalid"));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertEquals(List.of("allowed", "notList"), result.getChildrenConfig().stream().map(c -> c.get("key")).toList());
        assertEquals(Map.of("allowed", List.of(Map.of("id", 2))), result.getRecordData().get("children"));
    }

    @Test
    void unavailableRuntimeConfigKeepsFormFallbackAndRemovesUnconfiguredChildren() {
        record.put("children", Map.of("items", List.of(Map.of("id", 1))));
        when(data.getRuntimeValues("order-config")).thenThrow(new IllegalStateException("runtime unavailable"));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertTrue(result.getConfigured());
        assertEquals("Example", result.getRecordData().get("name"));
        assertTrue(result.getChildrenConfig().isEmpty());
        assertFalse(result.getRecordData().containsKey("children"));
        assertTrue(record.containsKey("children"));
    }

    @Test
    void unauthorizedTaskStopsBeforeRuntimeConfigAndRecordReads() {
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of("status", 1, "assignee", "43")));
        assertThrows(BusinessException.class, () -> service.getTaskFormContext(query()));
        verifyNoInteractions(bindings, configs, objects, data);
    }

    private void configure(List<Map<String, Object>> children) {
        config.setOptions(JSON.toJSONString(Map.of("masterDetailConfig", Map.of("children", children))));
    }

    private Map<String, Object> child(String identity, String key) {
        Map<String, Object> child = new LinkedHashMap<>();
        child.put(identity, key);
        child.put("fields", List.of(Map.of("field", "name")));
        return child;
    }

    private BusinessTaskFormContextQueryDTO query() {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId("task");
        query.setObjectCode("order");
        query.setRecordId(19L);
        return query;
    }
}
