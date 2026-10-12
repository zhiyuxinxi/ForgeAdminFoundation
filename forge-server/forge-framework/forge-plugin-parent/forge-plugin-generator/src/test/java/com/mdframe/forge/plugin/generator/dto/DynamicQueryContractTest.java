package com.mdframe.forge.plugin.generator.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeQueryPlans;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DynamicQueryContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void preservesMissingDefaultsAndExplicitNulls() throws Exception {
        CustomQueryExecuteDTO missing = mapper.readValue("{}", CustomQueryExecuteDTO.class);
        assertEquals(1, missing.getPageNum());
        assertEquals(10, missing.getPageSize());
        assertNull(missing.getConditions());
        CustomQueryExecuteDTO explicit = mapper.readValue(
                "{\"pageNum\":null,\"pageSize\":null,\"conditions\":[]}", CustomQueryExecuteDTO.class);
        assertNull(explicit.getPageNum());
        assertNull(explicit.getPageSize());
        assertEquals(0, explicit.getConditions().size());
        assertNull(mapper.readValue("{}", DynamicCrudQuery.class).getSearchParams());
    }

    @Test
    void preservesQueryJsonAndGeneratorMethodSignatures() throws Exception {
        String customJson = """
                {"pageNum":2,"pageSize":25,"fields":["name","amount"],
                 "conditions":[{"relation":"AND","field":"amount","operator":"between",
                   "value":10,"valueEnd":20,"includeChildren":false},
                  {"relation":"OR","field":"nodeId","operator":"in",
                   "value":["a","b"],"valueEnd":null,"includeChildren":true}],
                 "orderByColumn":"amount","isAsc":"desc","renderMode":"card"}
                """;
        CustomQueryExecuteDTO custom = mapper.readValue(customJson, CustomQueryExecuteDTO.class);
        assertEquals(mapper.readTree(customJson), mapper.valueToTree(custom));
        assertEquals(CustomQueryConditionDTO.class, custom.getConditions().get(0).getClass());
        assertEquals(custom, mapper.readValue(mapper.writeValueAsString(custom), CustomQueryExecuteDTO.class));

        String searchJson = """
                {"searchParams":{"name":"sample","nodeId":[1,2],"enabled":true,"empty":null},
                 "searchTypeMap":{"name":"like","nodeId":"in"}}
                """;
        DynamicCrudQuery query = mapper.readValue(searchJson, DynamicCrudQuery.class);
        assertEquals(mapper.readTree(searchJson), mapper.valueToTree(query));
        assertEquals(query, mapper.readValue(mapper.writeValueAsString(query), DynamicCrudQuery.class));
        assertEquals(DynamicCrudQuery.class, RuntimeQueryPlans.class
                .getMethod("search", LowcodeRuntimeConfig.class, DynamicCrudQuery.class).getParameterTypes()[1]);
        assertEquals(CustomQueryExecuteDTO.class, RuntimeQueryPlans.class
                .getMethod("custom", LowcodeRuntimeConfig.class, CustomQueryExecuteDTO.class).getParameterTypes()[1]);
    }
}
