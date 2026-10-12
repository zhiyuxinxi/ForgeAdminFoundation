package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 默认聚合明细记录读取器。
 */
@Component
@RequiredArgsConstructor
public class DynamicDetailRecordFetcher implements DetailRecordFetcher {

    private final DynamicCrudRepository repository;

    @Override
    public List<Map<String, Object>> fetchDetailRecords(String targetTableName,
                                                        String joinField,
                                                        Object joinValue,
                                                        Long tenantId) {
        return repository.selectListByColumn(targetTableName, joinField, joinValue);
    }

    @Override
    public List<Map<String, Object>> fetchDetailRecordsBatch(String targetTableName,
                                                             String joinField,
                                                             Collection<?> joinValues,
                                                             Long tenantId) {
        return repository.selectListByColumnIn(targetTableName, joinField, joinValues);
    }
}
