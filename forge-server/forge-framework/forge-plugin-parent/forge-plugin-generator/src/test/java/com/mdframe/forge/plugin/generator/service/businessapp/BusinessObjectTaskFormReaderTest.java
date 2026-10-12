package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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

class BusinessObjectTaskFormReaderTest {
    private final FlowClient flow = mock(FlowClient.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final Map<String, Object> record = new LinkedHashMap<>();
    private final BusinessObjectVO object = new BusinessObjectVO();
    private final AiBusinessBinding binding = new AiBusinessBinding();
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
    void runtimeDefaultFieldsRespectPermissionsLayoutAndConfiguredAssetOrder() {
        JSONObject designer = JSON.parseObject(object.getDesignerOptions());
        designer.getJSONObject("formDesignerSchema").put("settings", Map.of("layout",
                Map.of("gridCols", 2, "labelPlacement", "top", "labelWidth", "40")));
        object.setDesignerOptions(designer.toJSONString());
        JSONObject policy = JSON.parseObject(binding.getBindingConfig());
        JSONObject node = policy.getJSONArray("nodeForms").getJSONObject(0);
        node.put("allowApprove", false);
        node.put("formRef", Map.of("applicationId", " app ", "pageCode", "page"));
        node.put("fieldPermissions", List.of(Map.of("field", "name", "readable", true, "writable", false),
                Map.of("field", "secret", "readable", false)));
        binding.setBindingConfig(policy.toJSONString());
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(Map.of(
                "formType", "business-object", "formKey", "order", "allowApprove", false,
                "formFieldPermissions", node.get("fieldPermissions"), "formRef", node.get("formRef"))));
        config.setEditSchema(JSON.toJSONString(List.of(Map.of("field", "name", "label", "Runtime Name"),
                Map.of("field", "secret", "label", "Secret"))));
        List<Map<String, Object>> assets = List.of(Map.of("formKey", "second"), Map.of("formKey", "first"));
        config.setOptions(JSON.toJSONString(Map.of("formAssets", assets, "editGridCols", 9,
                "editLabelPlacement", "left", "editLabelWidth", "90")));
        record.put("secret", "hidden");

        BusinessTaskFormContextVO result = service.getTaskFormContext(query());

        assertTrue(result.getConfigured());
        assertEquals(2, result.getGridCols());
        assertEquals("top", result.getLabelPlacement());
        assertEquals("40", result.getLabelWidth());
        assertEquals(assets, result.getFormAssets());
        assertEquals("Runtime Name", result.getFields().get(0).get("label"));
        assertEquals(List.of("name"), result.getFields().stream().map(f -> f.get("field")).toList());
        assertEquals(Map.of("name", "Example"), result.getRecordData());
        assertEquals(false, result.getFieldPermissions().get(0).get("writable"));
        assertEquals(false, result.getAllowApprove());
        assertEquals("app", result.getApplicationId());
        assertEquals("page", result.getPageCode());
        assertEquals("hidden", record.get("secret"));
        var reads = inOrder(data);
        reads.verify(data).getRuntimeValues("order-config");
        reads.verify(data).selectById("order-config", 19L);
        reads.verify(data, times(3)).getRuntimeValues("order-config");
        reads.verifyNoMoreInteractions();
    }

    @Test
    void namedNonDefaultFormKeepsOwnFieldsAndExcludesOnlyActiveRelatedAsset() {
        object.setDesignerOptions("""
                {"formDesignerSchema":{"defaultFormKey":"order","forms":[
                {"formKey":"special","schema":{"formKey":"special","fields":[{"field":"name","label":"Special"}]}}]}}
                """);
        config.setEditSchema("[{\"field\":\"wrong\",\"label\":\"Runtime Only\"}]");
        config.setOptions("""
                {"editGridCols":3,"editLabelWidth":"75","formDesignerSchema":{"defaultFormKey":"order","forms":[
                {"formKey":"special","schema":{}},{"formKey":"other","usage":["detail"],"schema":{"fields":[]}},
                {"formKey":"last","formName":"Last","schema":{}},{"schema":{}}]}}
                """);
        BusinessTaskFormContextQueryDTO query = query();
        query.setFormKey("special");

        BusinessTaskFormContextVO result = service.getTaskFormContext(query);

        assertEquals("special", result.getFormKey());
        assertEquals("Special", result.getFields().get(0).get("label"));
        assertEquals(List.of("other", "last"), result.getFormAssets().stream().map(a -> a.get("formKey")).toList());
        assertEquals("other", result.getFormAssets().get(0).get("formName"));
        assertEquals(List.of("detail"), result.getFormAssets().get(0).get("usage"));
        assertEquals(3, result.getGridCols());
        assertEquals("75", result.getLabelWidth());
        assertEquals("left", result.getLabelPlacement());
    }

    @Test
    void unavailablePresentationConfigFallsBackButRecordFailureStillPropagates() {
        when(data.getRuntimeValues("order-config")).thenThrow(new IllegalStateException("config unavailable"));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertTrue(result.getConfigured());
        assertEquals("Name", result.getFields().get(0).get("label"));
        assertEquals(1, result.getGridCols());
        assertEquals("100", result.getLabelWidth());
        assertTrue(result.getFormAssets().isEmpty());
        assertEquals(Map.of("name", "Example"), result.getRecordData());

        IllegalStateException failure = new IllegalStateException("record read failed");
        when(data.selectById("order-config", 19L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getTaskFormContext(query())));
    }

    @Test
    void readonlyViewMasksProjectedFieldsWithoutChangingRuntimeSchema() {
        config.setEditSchema("[{\"field\":\"name\",\"label\":\"Name\",\"writable\":true}]");
        String original = config.getEditSchema();
        BusinessTaskFormContextVO result = service.getTaskFormReadonlyContext(query());
        assertEquals(false, result.getFields().get(0).get("writable"));
        assertEquals(true, result.getFields().get(0).get("readonly"));
        assertEquals(true, result.getFields().get(0).get("disabled"));
        assertEquals(false, result.getFieldPermissions().get(0).get("writable"));
        assertEquals(original, config.getEditSchema());
    }

    private BusinessTaskFormContextQueryDTO query() {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId("task");
        query.setObjectCode("order");
        query.setRecordId(19L);
        return query;
    }
}
