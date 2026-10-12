package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessRecordSelectorQueryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessRecordSelector search params")
class BusinessRecordSelectorSearchParamsTest {

    private final BusinessRecordSelectorService service = new BusinessRecordSelectorService(null, null, null);

    @Test
    @DisplayName("keeps single keyword field behavior")
    void keepsSingleKeywordFieldBehavior() throws Exception {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setKeyword("ABC");
        query.setKeywordFields(List.of("name"));

        Map<String, Object> params = buildSearchParams(query);

        assertEquals(Map.of("name", "ABC"), params);
    }

    @Test
    @DisplayName("normalizes multiple keyword fields into OR like conditions")
    void normalizesMultipleKeywordFieldsIntoOrLikeConditions() throws Exception {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setKeyword("mat");
        query.setKeywordFields(List.of("code", "name", "tenant_id", "code"));

        Map<String, Object> params = buildSearchParams(query);

        assertEquals(1, params.size());
        List<?> conditions = (List<?>) params.get(BusinessRecordSelectorService.OR_LIKE_SEARCH_KEY);
        assertEquals(2, conditions.size());
        assertEquals(Map.of("field", "code", "value", "mat"), conditions.get(0));
        assertEquals(Map.of("field", "name", "value", "mat"), conditions.get(1));
    }

    @Test
    @DisplayName("filters blank values and internal fields")
    void filtersBlankValuesAndInternalFields() throws Exception {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setSearchParams(Map.of(
                "warehouseId", 10,
                "supplierId", " ",
                "tenant_id", 1,
                "_secret", "hidden"
        ));

        Map<String, Object> params = buildSearchParams(query);

        assertEquals(Map.of("warehouseId", 10), params);
        assertFalse(params.containsKey("tenant_id"));
        assertFalse(params.containsKey("_secret"));
    }

    @Test
    @DisplayName("ignores empty keyword")
    void ignoresEmptyKeyword() throws Exception {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setKeyword(" ");
        query.setKeywordFields(List.of("code", "name"));

        Map<String, Object> params = buildSearchParams(query);

        assertTrue(params.isEmpty());
    }

    @Test
    @DisplayName("resolves object code aliases")
    void resolvesObjectCodeAliases() {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setTargetObjectCode("PW_SUPPLIER");

        assertEquals("PW_SUPPLIER", service.resolveObjectCode(query));

        query.setObjectCode("PW_MATERIAL");
        assertEquals("PW_MATERIAL", service.resolveObjectCode(query));
    }

    @Test
    @DisplayName("resolves extended object code aliases")
    void resolvesExtendedObjectCodeAliases() {
        BusinessRecordSelectorQueryDTO query = new BusinessRecordSelectorQueryDTO();
        query.setTargetEntityCode("PW_WAREHOUSE");
        assertEquals("PW_WAREHOUSE", service.resolveObjectCode(query));

        query = new BusinessRecordSelectorQueryDTO();
        query.setRefObjectCode("PW_SUPPLIER");
        assertEquals("PW_SUPPLIER", service.resolveObjectCode(query));

        query = new BusinessRecordSelectorQueryDTO();
        query.setTargetCode("PW_MATERIAL");
        assertEquals("PW_MATERIAL", service.resolveObjectCode(query));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSearchParams(BusinessRecordSelectorQueryDTO query) throws Exception {
        Method method = BusinessRecordSelectorService.class.getDeclaredMethod("buildSearchParams", BusinessRecordSelectorQueryDTO.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(service, query);
    }
}
