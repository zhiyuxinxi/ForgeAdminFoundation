package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the existing binding/read/save normalization contract during extraction. */
class NodeFormConfigurationTest {
    private BusinessFlowService service;

    @BeforeEach
    void setUp() {
        service = new BusinessFlowService(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void nodeFormsKeepFirstTaskAndDefaultModesWithoutMutatingInput() {
        Map<String, Object> source = values("taskDefKey", " approval ", "taskName", " Review ");
        List<Map<String, Object>> input = new ArrayList<>();
        input.add(null);
        input.add(values("taskDefKey", " "));
        input.add(source);
        input.add(values("taskDefKey", "approval", "formMode", "EXTERNAL"));
        List<Map<String, Object>> result = invoke("normalizeNodeForms", input);
        assertEquals(List.of(Map.of("taskDefKey", "approval", "taskName", "Review",
                "formMode", "BUSINESS_OBJECT_FORM", "editMode", "READONLY")), result);
        assertEquals(" approval ", source.get("taskDefKey"));
        assertFalse(source.containsKey("formMode"));
        assertNotSame(source, result.get(0));
        assertEquals(List.of(), invoke("normalizeNodeForms", (Object) null));
    }

    @Test
    void modeAliasesKeepSupportedModesAndRejectUnknownValues() {
        assertEquals("BUSINESS_CODE_FORM", invoke("normalizeNodeFormMode", " business_code_form "));
        assertEquals("EXTERNAL", invoke("normalizeNodeFormMode", "external"));
        assertEquals("BUSINESS_OBJECT_FORM", invoke("normalizeNodeFormMode", "script"));
        assertEquals("EDITABLE", invoke("normalizeNodeEditMode", " editable "));
        assertEquals("MODIFY_RESUBMIT", invoke("normalizeNodeEditMode", "modify_resubmit"));
        assertEquals("READONLY", invoke("normalizeNodeEditMode", "unknown"));
    }

    @Test
    void explicitPermissionAliasesKeepFirstAndNormalizeBooleanInputs() {
        Map<String, Object> first = values("fieldCode", " amount ", "label", " Total ",
                "visible", "yes", "editable", 1, "required", "1");
        List<Map<String, Object>> result = permissions(List.of(first,
                values("code", "amount", "readable", false), values("field", " ")));
        assertEquals(1, result.size());
        assertEquals("amount", result.get(0).get("field"));
        assertEquals("amount", result.get(0).get("fieldCode"));
        assertEquals("Total", result.get(0).get("label"));
        assertPolicy(result.get(0), true, true, true);
        assertFalse(first.containsKey("writable"));
    }

    @Test
    void unreadableOrReadonlyFieldsCannotBecomeWritableOrRequired() {
        List<Map<String, Object>> result = permissions(List.of(
                values("field", "secret", "readable", false, "visible", true,
                        "writable", true, "required", true),
                values("field", "locked", "writable", "no", "required", true),
                values("code", "ordinary")));
        assertPolicy(result.get(0), false, false, false);
        assertPolicy(result.get(1), true, false, false);
        assertPolicy(result.get(2), true, true, false);
    }

    @Test
    void fieldsArrayTakesPrecedenceOverLegacySelectionLists() {
        List<Map<String, Object>> result = permissions(Map.of("fields", List.of(
                values("field", "explicit", "writable", false)), "writableFields", List.of("legacy")));
        assertEquals(List.of("explicit"), result.stream().map(item -> item.get("field")).toList());
        assertPolicy(result.get(0), true, false, false);
    }

    @Test
    void selectionAliasesPreserveOrderAndRestrictWritableAndRequiredFields() {
        List<Map<String, Object>> result = permissions(values("visibleFields", List.of(" a ", "b"),
                "readable", "[\"b\",\"c\"]", "writable", List.of("b", "d"),
                "requiredFields", List.of("b", "d")));
        assertEquals(List.of("a", "b", "c", "d"), result.stream().map(item -> item.get("field")).toList());
        assertPolicy(result.get(0), true, false, false);
        assertPolicy(result.get(1), true, true, true);
        assertPolicy(result.get(2), true, false, false);
        assertPolicy(result.get(3), false, false, false);
        assertTrue(permissions(Map.of()).isEmpty());
    }

    @Test
    void emptyOrMalformedExplicitPolicyFallsBackToNodeSelections() {
        List<Map<String, Object>> result = invoke("normalizeNodeForms", List.of(values(
                "taskDefKey", "review", "fieldPermissions", "not-json", "writableFields", List.of("amount"),
                "requiredFields", List.of("amount"))));
        List<Map<String, Object>> policies = (List<Map<String, Object>>) result.get(0).get("fieldPermissions");
        assertEquals("amount", policies.get(0).get("field"));
        assertPolicy(policies.get(0), true, true, true);
        assertTrue(permissions("not-json").isEmpty());
    }

    @Test
    void formReferenceAndTextValuesKeepTheExistingConsumerShape() {
        JSONObject form = new JSONObject(Map.of("formKey", "shared"));
        List<Map<String, Object>> result = invoke("normalizeNodeForms", List.of(
                values("taskDefKey", "code", "formMode", "business_code_form", "formRef", form,
                        "providerKey", " provider ", "formUrl", "null"),
                values("taskDefKey", "object", "formRef", Map.of("formKey", "mapped")),
                values("taskDefKey", "text", "formRef", "{\"formKey\":\"ignored\"}")));
        assertSame(form, result.get(0).get("formRef"));
        assertEquals("provider", result.get(0).get("providerKey"));
        assertFalse(result.get(0).containsKey("formUrl"));
        assertTrue(result.get(1).get("formRef") instanceof JSONObject);
        assertFalse(result.get(2).containsKey("formRef"));
    }

    private List<Map<String, Object>> permissions(Object input) {
        return invoke("normalizeFieldPermissions", input);
    }

    private void assertPolicy(Map<String, Object> item, boolean readable, boolean writable, boolean required) {
        assertEquals(readable, item.get("visible"));
        assertEquals(readable, item.get("readable"));
        assertEquals(writable, item.get("editable"));
        assertEquals(writable, item.get("writable"));
        assertEquals(required, item.get("required"));
    }

    private Map<String, Object> values(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    private <T> T invoke(String name, Object... arguments) {
        return ReflectionTestUtils.invokeMethod(service, name, arguments);
    }
}
