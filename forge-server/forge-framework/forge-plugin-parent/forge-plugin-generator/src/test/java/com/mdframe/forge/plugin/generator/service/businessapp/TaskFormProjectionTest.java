package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
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

/** Exercises the existing service projection boundary with its real field and data rules. */
class TaskFormProjectionTest {
    private BusinessFlowService service;

    @BeforeEach
    void setUp() {
        service = new BusinessFlowService(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void fieldProjectionResolvesPermissionAliasesAndKeepsSourceUntouched() {
        Map<String, Object> source = field("totalAmount", "componentType", "money",
                "props", Map.of("dictType", "amount_kind", "disabled", true));
        List<Map<String, Object>> projected = fields(List.of(source), List.of(
                field("total_amount", "writable", true, "required", true)));
        Map<String, Object> item = projected.get(0);
        assertEquals("number", item.get("type"));
        assertEquals("money", item.get("componentType"));
        assertEquals("totalAmount", item.get("fieldCode"));
        assertEquals(true, item.get("writable"));
        assertEquals(true, item.get("required"));
        assertEquals(false, ((Map<?, ?>) item.get("props")).get("readonly"));
        assertEquals("amount_kind", ((Map<?, ?>) item.get("props")).get("dictType"));
        assertFalse(source.containsKey("writable"));
        assertEquals(true, ((Map<?, ?>) source.get("props")).get("disabled"));
    }

    @Test
    void unreadableFieldsDisappearAndMissingPermissionsRemainReadonly() {
        List<Map<String, Object>> projected = fields(List.of(field("hidden"), field("shown")),
                List.of(field("hidden", "readable", false)));
        assertEquals(1, projected.size());
        assertEquals("shown", projected.get(0).get("field"));
        assertEquals(false, projected.get(0).get("writable"));
        assertEquals(false, projected.get(0).get("required"));
        assertEquals(true, projected.get(0).get("disabled"));
    }

    @Test
    void firstPermissionAliasWinsAndReadonlyCannotBeRequired() {
        Map<String, Object> item = fields(List.of(field("totalAmount")), List.of(
                field("total_amount", "writable", false, "required", true),
                field("totalAmount", "writable", true))).get(0);
        assertEquals(false, item.get("writable"));
        assertEquals(false, item.get("required"));
    }

    @Test
    void fallbackPermissionsRespectFieldAndPropsGuardsAndExplicitPolicyWins() {
        List<Map<String, Object>> catalog = List.of(field("name", "required", true),
                field("locked", "props", "{\"readonly\":true}"), field("internal", "internal", true),
                field("name", "disabled", true));
        List<Map<String, Object>> normalized = invoke("normalizeBusinessObjectTaskPermissions", catalog, List.of());
        assertEquals(List.of("name", "locked"), normalized.stream().map(p -> p.get("field")).toList());
        assertEquals(true, normalized.get(0).get("required"));
        assertEquals(false, normalized.get(1).get("writable"));
        List<Map<String, Object>> explicit = List.of(field("name", "writable", false));
        assertSame(explicit, invoke("normalizeBusinessObjectTaskPermissions", catalog, explicit));
    }

    @Test
    void recordProjectionPreservesOnlyVisibleValuesAndReferenceDisplayFields() {
        List<Map<String, Object>> projected = fields(List.of(field("ownerId", "type", "userSelect",
                "props", Map.of("displayField", "ownerCaption"))), List.of());
        Map<String, Object> children = Map.of("items", List.of(Map.of("id", 1)));
        Map<String, Object> record = Map.of("owner_id", 7, "owner_name", "Alice", "ownerCaption", "Reviewer",
                "hidden", "secret", "children", children);
        Map<String, Object> result = invoke("filterVisibleRecordData", record, projected);
        assertEquals(7, result.get("ownerId"));
        assertEquals("Alice", result.get("ownerName"));
        assertEquals("Reviewer", result.get("ownerCaption"));
        assertSame(children, result.get("children"));
        assertFalse(result.containsKey("hidden"));
        assertFalse(result.containsKey("ownerIdName"));
    }

    @Test
    void topLevelNullOverridesMainRecordAndOrdinaryFieldsDoNotInventDisplayFields() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("first_name", null);
        record.put("firstNameName", "not-a-reference");
        record.put("main", Map.of("firstName", "fallback", "last_name", "Smith"));
        Map<String, Object> result = invoke("filterVisibleRecordData", record,
                fields(List.of(field("firstName"), field("lastName")), List.of()));
        assertTrue(result.containsKey("firstName"));
        assertEquals(null, result.get("firstName"));
        assertEquals("Smith", result.get("lastName"));
        assertFalse(result.containsKey("firstNameName"));
    }

    @Test
    void codeFormProjectionHidesInternalAndUnreadableFieldsWithoutMutatingProviderFields() {
        BusinessTaskFormContextVO context = new BusinessTaskFormContextVO();
        Map<String, Object> source = field("visible", "props", Map.of("readonly", true));
        context.setFields(new ArrayList<>(List.of(source, field("hidden"), field("internal", "internal", true),
                field("system", "systemField", true))));
        context.setRecordData(Map.of("visible", 1, "hidden", 2, "internal", 3, "system", 4));
        BusinessTaskFormContextVO result = invoke("applyBusinessCodeFieldPermissions", context,
                List.of(field("visible", "writable", true), field("hidden", "readable", false)));
        assertSame(context, result);
        assertEquals(List.of("visible"), result.getFields().stream().map(f -> f.get("field")).toList());
        assertEquals(Map.of("visible", 1), result.getRecordData());
        assertNotSame(source, result.getFields().get(0));
        assertFalse(source.containsKey("writable"));
        // Provider readonly props are preserved; the original code-form contract only updates disabled.
        assertEquals(true, ((Map<?, ?>) result.getFields().get(0).get("props")).get("readonly"));
        assertEquals(false, ((Map<?, ?>) result.getFields().get(0).get("props")).get("disabled"));
    }

    @Test
    void readonlyViewMasksBothFieldsAndPermissionsWithoutChangingRequiredOrNestedProps() {
        BusinessTaskFormContextVO context = new BusinessTaskFormContextVO();
        Map<String, Object> item = field("name", "writable", true, "required", true,
                "props", Map.of("disabled", false));
        context.setFields(new ArrayList<>(List.of(item)));
        context.setFieldPermissions(new ArrayList<>(List.of(field("name", "writable", true))));
        invoke("makeBusinessTaskFormReadonly", context);
        assertEquals(false, item.get("writable"));
        assertEquals(true, item.get("required"));
        assertEquals(true, context.getFieldPermissions().get(0).get("disabled"));
        assertEquals(false, ((Map<?, ?>) item.get("props")).get("disabled"));
    }

    private List<Map<String, Object>> fields(List<Map<String, Object>> catalog, List<Map<String, Object>> permissions) {
        return invoke("buildTaskFormFields", catalog, permissions);
    }

    private <T> T invoke(String method, Object... args) {
        return ReflectionTestUtils.invokeMethod(service, method, args);
    }

    private Map<String, Object> field(String name, Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field", name);
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
