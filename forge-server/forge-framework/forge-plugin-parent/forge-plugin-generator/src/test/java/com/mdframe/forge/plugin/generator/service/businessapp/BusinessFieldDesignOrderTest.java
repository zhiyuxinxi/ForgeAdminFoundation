package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessFieldDesignOrderTest {
    private final BusinessObjectDesignerService designer = mock(BusinessObjectDesignerService.class);
    private final LowcodeModelSchemaNormalizer normalizer = mock(LowcodeModelSchemaNormalizer.class);
    private final BusinessFieldSchemaService fields = new BusinessFieldSchemaService(
            new LowcodeModelSchemaNormalizer(), new BusinessNamingService());
    private final BusinessFieldDesignService service = new BusinessFieldDesignService(
            designer, fields, mock(LowcodeDdlService.class), normalizer, new ObjectMapper());
    private final BusinessObjectDesignerService.DesignerContext context =
            new BusinessObjectDesignerService.DesignerContext();

    @BeforeEach
    void draftWithDesignerOrder() {
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setFields(new ArrayList<>(List.of(field("note"), field("name"), field("amount"))));
        context.setModelSchema(model);
        LowcodePageSchema page = new LowcodePageSchema();
        page.setZones(new ArrayList<>());
        for (String key : List.of("search", "table", "edit", "detail")) {
            LowcodePageZone zone = new LowcodePageZone();
            zone.setZoneKey(key);
            zone.setFieldRefs(List.of("amount", "note", "name"));
            page.getZones().add(zone);
        }
        context.setPageSchema(page);
        when(designer.loadContext(1L)).thenReturn(context);
        when(normalizer.normalizeModelFields(any(), eq(true))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void updatingPlaceholderKeepsEveryDesignedZonePosition() {
        BusinessFieldDTO update = new BusinessFieldDTO();
        update.setBasicProps(Map.of("placeholder", "Enter a note"));
        service.updateField(1L, "note", update);

        assertOrder("amount", "note", "name");
        assertEquals("Enter a note", context.getModelSchema().getFields().get(0).getBasicProps().get("placeholder"));
        verify(designer).saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    @Test
    void renamingAnUnstoredFieldKeepsItsDesignedPosition() {
        BusinessFieldDTO update = new BusinessFieldDTO();
        update.setFieldCode("memo");
        update.setColumnName("memo");
        service.updateField(1L, "note", update);

        assertOrder("amount", "memo", "name");
    }

    @Test
    void hidingAFieldRemovesItWithoutReorderingOtherFields() {
        BusinessFieldDTO update = new BusinessFieldDTO();
        update.setFieldStatus("HIDDEN");
        service.updateField(1L, "note", update);

        assertOrder("amount", "name");
    }

    @Test
    void showingAnAbsentFieldAppendsItOnlyOnce() {
        context.getPageSchema().getZones().forEach(zone -> zone.setFieldRefs(List.of("amount", "name")));
        service.updateField(1L, "note", new BusinessFieldDTO());
        service.updateField(1L, "note", new BusinessFieldDTO());

        assertOrder("amount", "name", "note");
    }

    private LowcodeFieldSchema field(String code) {
        BusinessFieldDTO dto = new BusinessFieldDTO();
        dto.setFieldCode(code);
        dto.setColumnName(code);
        dto.setFieldName(code);
        dto.setFieldType("TEXT");
        dto.setSearchable(true);
        dto.setListVisible(true);
        dto.setFormVisible(true);
        return fields.buildFieldSchema(dto);
    }

    private void assertOrder(String... expected) {
        for (LowcodePageZone zone : context.getPageSchema().getZones()) {
            if (!"toolbar".equals(zone.getZoneKey())) {
                assertEquals(List.of(expected), zone.getFieldRefs(), zone.getZoneKey());
            }
        }
    }
}
