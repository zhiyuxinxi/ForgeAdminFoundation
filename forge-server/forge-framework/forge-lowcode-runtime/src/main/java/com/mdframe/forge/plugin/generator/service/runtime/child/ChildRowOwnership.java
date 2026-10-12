package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.starter.core.exception.BusinessException;
import java.util.Map;

/** Ownership checks and existing child deletion operations within the caller transaction. */
public final class ChildRowOwnership {
    private final DynamicCrudRepository repository;

    public ChildRowOwnership(DynamicCrudRepository repository) {
        this.repository = repository;
    }

    public boolean deleteMarkedRow(RuntimeChildRelation relation, Object rowId, Object relationValue) {
        if (rowId == null) {
            return false;
        }
        assertChildRowBelongsToMain(relation, rowId, relationValue);
        int affected = repository.deleteById(relation.tableName(), "id", rowId,
                repository.hasDelFlag(relation.tableName()), null);
        if (affected <= 0) {
            throw new BusinessException("子表行删除失败或数据不存在");
        }
        return true;
    }

    public void deleteForReplacement(RuntimeChildRelation relation, Object relationValue) {
        repository.deleteByColumn(relation.tableName(), relation.childFkColumn(), relationValue,
                repository.hasDelFlag(relation.tableName()));
    }

    public void assertChildRowBelongsToMain(RuntimeChildRelation relation, Object rowId, Object relationValue) {
        Map<String, Object> existing = repository.selectById(relation.tableName(), "id", rowId, null);
        if (existing == null || !sameValue(existing.get(relation.childFkColumn()), relationValue)) {
            throw new BusinessException("子表行不存在或不属于当前主记录");
        }
    }

    private boolean sameValue(Object left, Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        return String.valueOf(left).equals(String.valueOf(right));
    }
}
