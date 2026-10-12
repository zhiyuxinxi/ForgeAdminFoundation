package com.mdframe.forge.plugin.generator.dto.lowcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeModelSchemaContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void runtimeReaderKeepsCompleteNestedContractAndLegacyFieldAliases() throws Exception {
        LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
        config.setModelSchema("""
            {
              "schemaVersion":2,"appType":"MASTER_DETAIL","tableName":"orders",
              "businessNameI18n":{"zh-CN":"订单","en-US":"Orders"},
              "domain":{"id":31,"code":"sales","name":"Sales"},
              "object":{"code":"order","name":"Order"},
              "sourceTable":{"tableId":7,"datasourceId":8,"tableName":"orders"},
              "runtimeDatasource":{"datasourceId":8,"allowWrite":true,"allowDdl":false,"readonly":false},
              "primaryKey":{"field":"id","columnName":"id","autoIncrement":true},
              "tenantStrategy":{"mode":"FORGE_TENANT_ID","columnName":"tenant_id"},
              "auditStrategy":{"mode":"FORGE_COLUMNS","createByColumn":"create_by"},
              "logicDeleteStrategy":{"mode":"DEL_FLAG","columnName":"del_flag","activeValue":"0","deletedValue":"id"},
              "treeConfig":{"keyField":"id","parentField":"parentId","enabled":false},
              "fields":[{"fieldCode":"unit_price","fieldName":"Price","fieldType":"DECIMAL",
                "fieldLength":"12,2","displayInList":true,"displayInForm":false,
                "labelI18n":{"zh-CN":"单价","en-US":"Unit price"},"defaultValue":0}],
              "relations":[{"relationType":"MANY_TO_ONE","targetObjectCode":"customer","sourceField":"customerId"}],
              "indexes":[{"indexName":"ix_price","fields":["unitPrice"]}],
              "uniqueConstraints":[{"name":"uq_price","fields":["unitPrice"],"scope":"TENANT","normalize":["trim"]}],
              "validationRules":[{"type":"UNIQUE","fields":["unitPrice"]}],
              "policies":{"dataScope":"TENANT","auditEnabled":false},
              "children":[{"tableName":"order_lines","fields":[{"field":"sku","columnName":"sku"}]}]
            }
            """);
        LowcodeModelSchema model = new RuntimeFieldMetadata(mapper).model(config);
        assertNotNull(model);
        JsonNode json = mapper.valueToTree(model);
        assertEquals("sales", json.at("/domain/code").asText());
        assertEquals("order", json.at("/object/code").asText());
        assertEquals(7, json.at("/sourceTable/tableId").asInt());
        assertTrue(json.at("/runtimeDatasource/allowWrite").asBoolean());
        assertFalse(json.at("/runtimeDatasource/allowDdl").asBoolean());
        assertEquals("id", json.at("/primaryKey/field").asText());
        assertEquals("tenant_id", json.at("/tenantStrategy/columnName").asText());
        assertEquals("create_by", json.at("/auditStrategy/createByColumn").asText());
        assertEquals("id", json.at("/logicDeleteStrategy/deletedValue").asText());
        assertFalse(json.at("/treeConfig/enabled").asBoolean());
        assertEquals("unitPrice", json.at("/fields/0/field").asText());
        assertEquals("unit_price", json.at("/fields/0/columnName").asText());
        assertEquals("decimal", json.at("/fields/0/dataType").asText());
        assertEquals(12, json.at("/fields/0/length").asInt());
        assertEquals(2, json.at("/fields/0/precision").asInt());
        assertFalse(json.at("/fields/0/formVisible").asBoolean());
        assertEquals("Unit price", json.at("/fields/0/labelI18n/en-US").asText());
        assertEquals("Orders", json.at("/businessNameI18n/en-US").asText());
        assertEquals("customer", json.at("/relations/0/targetObjectCode").asText());
        assertEquals("NORMAL", json.at("/indexes/0/indexType").asText());
        assertEquals("TENANT", json.at("/uniqueConstraints/0/scope").asText());
        assertEquals("UNIQUE", json.at("/validationRules/0/type").asText());
        assertFalse(json.at("/policies/auditEnabled").asBoolean());
        assertEquals("sku", json.at("/children/0/fields/0/field").asText());
        assertEquals(json, mapper.valueToTree(mapper.treeToValue(json, LowcodeModelSchema.class)));
    }

    @Test
    void missingCollectionsAndPoliciesKeepDefaultsWhileExplicitNullRemainsNull() {
        LowcodeRuntimeConfig config = new LowcodeRuntimeConfig();
        config.setModelSchema("{}");
        RuntimeFieldMetadata reader = new RuntimeFieldMetadata(mapper);
        LowcodeModelSchema model = reader.model(config);
        assertNotNull(model);
        assertTrue(model.getFields().isEmpty());
        assertTrue(model.getIndexes().isEmpty());
        assertTrue(model.getUniqueConstraints().isEmpty());
        assertTrue(model.getChildren().isEmpty());
        assertEquals("TENANT", model.getPolicies().getDataScope());
        assertEquals("tenant_id", model.getPolicies().getTenantColumn());
        assertTrue(model.getPolicies().getAuditEnabled());
        config.setModelSchema("{\"fields\":null,\"policies\":null}");
        LowcodeModelSchema explicitNull = reader.model(config);
        assertNotNull(explicitNull);
        assertNull(explicitNull.getFields());
        assertNull(explicitNull.getPolicies());
    }
}
