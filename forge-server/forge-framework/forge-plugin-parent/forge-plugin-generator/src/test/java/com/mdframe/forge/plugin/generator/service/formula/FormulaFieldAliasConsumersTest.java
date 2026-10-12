package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FormulaFieldAliasConsumersTest {

    @Test
    void allDatabaseConsumersResolveAliasesAndPreserveTenantAndColumn() {
        Fixture fixture = new Fixture(List.of(Map.of("customer_id", "c1", "real_name", "Alice")));
        Map<String, Object> source = Map.of("customer_id", "c1");

        assertEquals("Alice", fixture.aggregate(source).get(0).get("realName"));
        LookupResolveResult lookup = fixture.lookup("realName", source);
        assertTrue(lookup.isSuccess(), lookup.getErrorMessage());
        assertEquals("Alice", lookup.getValue());
        assertEquals("Alice", fixture.cross("realName", source));
        verify(fixture.fetcher, times(3)).fetchDetailRecords("biz_customer", "customer_id", "c1", 7L);
    }

    @Test
    void aliasesPreserveConflictsNullKeysAcronymsAndEscapedTextWithoutMutatingRows() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customer_id", "c1");
        row.put("real_name", "snake");
        row.put("realName", "camel");
        row.put("URLValue", "O'Reilly\\path");
        row.put("literal\\_name", "value");
        row.put("quoted'name", "quoted");
        row.put(null, "null-key");
        Map<String, Object> original = new LinkedHashMap<>(row);
        Fixture fixture = new Fixture(List.of(row));

        Map<String, Object> result = fixture.aggregate(Map.of("customerId", "c1")).get(0);
        assertEquals("snake", result.get("real_name"));
        assertEquals("camel", result.get("realName"));
        assertEquals("O'Reilly\\path", result.get("u_r_l_value"));
        assertEquals("value", result.get("literal\\Name"));
        assertEquals("quoted", result.get("quoted'name"));
        assertEquals("null-key", result.get(null));
        assertEquals(original, row);
        assertNotSame(row, result);
        assertEquals("camel", fixture.lookup("realName", Map.of("customerId", "c1")).getValue());
        assertEquals("camel", fixture.cross("realName", Map.of("customerId", "c1")));
    }

    @Test
    void exactNullSourceWinsButNullOutputAliasCanBeFilled() {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("customer_id", "c1");
        target.put("realName", null);
        target.put("real_name", "Alice");
        Fixture fixture = new Fixture(List.of(target));
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("customerId", null);
        source.put("customer_id", "c1");

        assertEquals("Alice", fixture.aggregate(source).get(0).get("realName"));
        verify(fixture.fetcher).fetchDetailRecords("biz_customer", "customer_id", null, 7L);
        LookupResolveResult lookup = fixture.lookup("realName", source);
        assertTrue(lookup.isSuccess(), lookup.getErrorMessage());
        assertFalse(lookup.isMatched());
        assertEquals("missing", lookup.getValue());
        assertNull(fixture.cross("realName", source));
        assertEquals("Alice", fixture.cross("realName", Map.of("customerId", "c1")));
    }

    @Test
    void strictUnknownFieldsStayFailuresWhileAggregateKeepsColumnFallback() {
        Fixture fixture = new Fixture(List.of(Map.of("customer_id", "c1", "real_name", "Alice")));
        Map<String, Object> source = Map.of("customerId", "c1");
        for (String field : List.of("missingField", "REALNAME")) {
            String message = "Target field not found in model schema: " + field;
            LookupResolveResult lookup = fixture.lookup(field, source);
            assertFalse(lookup.isSuccess());
            assertEquals(message, lookup.getErrorMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class,
                    () -> fixture.cross(field, source)).getMessage());
        }
        verifyNoInteractions(fixture.fetcher);
        fixture.relation.setTargetFieldCode("unmappedId");
        fixture.aggregate(source);
        verify(fixture.fetcher).fetchDetailRecords("biz_customer", "unmapped_id", "c1", 7L);
    }

    @Test
    void emptyResultsAndMalformedRowsRetainExistingConsumerFailureSemantics() {
        Fixture empty = new Fixture(null);
        Map<String, Object> source = Map.of("customerId", "c1");
        assertTrue(empty.aggregate(source).isEmpty());
        assertFalse(empty.lookup("realName", source).isMatched());

        Fixture malformed = new Fixture(Collections.singletonList(null));
        assertThrows(NullPointerException.class, () -> malformed.aggregate(source));
        assertFalse(malformed.lookup("realName", source).isSuccess());
        assertThrows(NullPointerException.class, () -> malformed.cross("realName", source));
    }

    private static final class Fixture {
        private final BusinessObjectRelationMapper relations = mock(BusinessObjectRelationMapper.class);
        private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
        private final AiLowcodeModelMapper models = mock(AiLowcodeModelMapper.class);
        private final DetailRecordFetcher fetcher = mock(DetailRecordFetcher.class, CALLS_REAL_METHODS);
        private final AiBusinessObjectRelation relation = new AiBusinessObjectRelation();
        private final ObjectMapper json = new ObjectMapper();

        private Fixture(List<Map<String, Object>> rows) {
            relation.setId(1L);
            relation.setSuiteCode("default");
            relation.setSourceObjectCode("order");
            relation.setTargetObjectCode("customer");
            relation.setSourceFieldCode("customerId");
            relation.setTargetFieldCode("customerId");
            when(relations.selectRelationById(7L, 1L)).thenReturn(relation);
            AiBusinessObject object = new AiBusinessObject();
            object.setModelCode("customer");
            when(objects.selectByObjectCode(7L, "default", "customer")).thenReturn(object);
            AiLowcodeModel model = new AiLowcodeModel();
            model.setModelSchema("""
                    {"tableName":"biz_customer","fields":[
                      {"field":"customerId","columnName":"customer_id"},
                      {"field":"realName","columnName":"real_name"}]}
                    """);
            when(models.selectByModelCode(7L, "customer")).thenReturn(model);
            doReturn(rows).when(fetcher).fetchDetailRecords(anyString(), anyString(), any(), eq(7L));
        }

        private Map<String, Object> context(Map<String, Object> row) {
            return Map.of("__formulaRuntimeContext__", runtime(row));
        }

        private FormulaRuntimeContext runtime(Map<String, Object> row) {
            return new FormulaRuntimeContext(7L, "default", "order", row);
        }

        private List<Map<String, Object>> aggregate(Map<String, Object> row) {
            return new DbAggregateDataProvider(relations, objects, models, fetcher, json)
                    .getDetailRecords("1", context(row));
        }

        private LookupResolveResult lookup(String field, Map<String, Object> row) {
            LookupConfig config = new LookupConfig("1", "customer", "customerId", "customerId", field, "missing");
            return new FormulaLookupResolver(relations, objects, models, fetcher, json).resolve(config, context(row));
        }

        private Object cross(String field, Map<String, Object> row) {
            CrossObjectConfig config = new CrossObjectConfig("customer." + field, "1", "customer", field, null);
            return new FormulaCrossObjectResolver(relations, objects, models, fetcher, json)
                    .resolve(config, row, runtime(row));
        }
    }
}
