package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessDocumentStatusRowsTest {
    private final Map<String, String> legacy = Map.of("DRAFT", "legacy-draft");

    @Test
    void currentRowsOverrideLegacyAndKeepActionPolicy() {
        Map<String, Object> row = Map.of("standardStatus", "DRAFT", "statusValue", "current",
                "allowEdit", false, "allowDelete", false);
        var draft = read(new ObjectMapper(), List.of(row)).get(0);
        assertEquals("current", draft.getStatusValue());
        assertFalse(draft.getAllowEdit());
        assertFalse(draft.getAllowDelete());
    }

    @Test
    void absentOrEmptyRowsKeepTheLegacyMapping() {
        assertEquals("legacy-draft", read(new ObjectMapper(), null).get(0).getStatusValue());
        assertEquals("legacy-draft", read(new ObjectMapper(), List.of()).get(0).getStatusValue());
    }

    @Test
    void incompatibleSerializedShapeKeepsCompatibilityFallback() {
        assertEquals("legacy-draft", read(new ObjectMapper(), "old-format").get(0).getStatusValue());
    }

    @Test
    void businessFailureDoesNotSilentlySelectLegacyRules() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        BusinessException failure = new BusinessException("invalid business mapping");
        when(mapper.convertValue(any(), any(TypeReference.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> read(mapper, List.of())));
    }

    @Test
    void invalidNormalizedRowDoesNotSilentlySelectLegacyRules() {
        assertThrows(NullPointerException.class, () -> read(new ObjectMapper(), Arrays.asList((Object) null)));
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> read(ObjectMapper mapper, Object raw) {
        var service = new BusinessDocumentConfigService(null, null, null, null, mapper, null);
        return ReflectionTestUtils.invokeMethod(service, "readStatusRows",
                raw == null ? Map.of() : Map.of("statusMappingRows", raw), legacy);
    }
}
