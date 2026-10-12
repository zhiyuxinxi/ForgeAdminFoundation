package com.mdframe.forge.plugin.data.controller;

import com.mdframe.forge.plugin.data.dto.DataDatasetPreviewDTO;
import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.service.DataDatasetRuntimeService;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataDatasetPreviewBoundaryTest {
    @Mock private DataDatasetRuntimeService runtime;
    @InjectMocks private DataDatasetController controller;

    @Test
    void savedPreviewMustUseProtectedRuntimeAndRetainResponseShape() {
        DataDatasetQueryResultVO result = new DataDatasetQueryResultVO();
        result.setDimensions(List.of("phone"));
        result.setSource(List.of(Map.of("phone", "13****78")));
        result.setTotal(5L);
        when(runtime.query(any())).thenReturn(result);
        DataDatasetPreviewDTO request = new DataDatasetPreviewDTO();
        request.setDatasetId(999L);
        request.setMaxRows(10);
        request.setParams(Map.of("id", 2));
        Map<String, Object> response = controller.preview(30L, request).getData();
        assertEquals(Map.of("columns", List.of("phone"), "rows", result.getSource(), "total", 5L), response);
        ArgumentCaptor<DataDatasetQueryDTO> query = ArgumentCaptor.forClass(DataDatasetQueryDTO.class);
        verify(runtime).query(query.capture());
        assertEquals(30L, query.getValue().getDatasetId());
        assertEquals(request.getParams(), query.getValue().getParams());
        assertEquals(10, query.getValue().getMaxRows());
    }

    @Test
    void previewMustNotSwallowAccessFailureOrFallBackToRawSql() {
        when(runtime.query(any())).thenThrow(new BusinessException("denied"));
        assertThrows(BusinessException.class, () -> controller.preview(30L, new DataDatasetPreviewDTO()));
    }
}
