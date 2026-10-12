package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.starter.core.exception.BusinessException;
import java.util.List;
import java.util.Map;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowValidator.validateChildRow;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.filterChildWriteData;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.hasWritableChildData;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isDeletedChildRow;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isMergeChildSaveMode;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.normalizeChildRows;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.resolveChildRowId;

/** Executes child insert, merge, replace and joined writes inside the parent transaction. */
public final class ChildRowWriter {
    private final DynamicCrudRepository repository;
    private final ChildRowOwnership ownership;

    public ChildRowWriter(DynamicCrudRepository repository, ChildRowOwnership ownership) {
        this.repository = repository;
        this.ownership = ownership;
    }

    public record ParentRecord(String tableName, Object id, Map<String, Object> written,
                               Map<String, Object> existing) {
    }

    public boolean insertMasterDetails(ParentRecord parent, List<RuntimeChildRelation> relations,
                                     Map<String, Object> childrenPayload) {
        Map<String, Object> currentMainRecord = null;
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : relations) {
            List<Map<String, Object>> childRows = normalizeChildRows(childrenPayload.get(relation.modelCode()));
            if (childRows.isEmpty()) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !parent.written().containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(parent.tableName(), parent.id());
            }
            Object relationValue = relation.mainValue(parent.written(), parent.id(), currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            for (Map<String, Object> row : childRows) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
                childrenChanged = true;
            }
        }
        return childrenChanged;
    }

    public boolean updateMasterDetails(ParentRecord parent, List<RuntimeChildRelation> relations,
                                     Map<String, Object> childrenPayload) {
        Map<String, Object> currentMainRecord = parent.existing();
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : relations) {
            if (!childrenPayload.containsKey(relation.modelCode())) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !parent.written().containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(parent.tableName(), parent.id());
            }
            Object relationValue = relation.mainValue(parent.written(), parent.id(), currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            if (isMergeChildSaveMode(relation)) {
                childrenChanged = mergeMasterDetailChildRows(
                        relation,
                        relationValue,
                        normalizeChildRows(childrenPayload.get(relation.modelCode()))
                ) || childrenChanged;
                continue;
            }
            ownership.deleteForReplacement(relation, relationValue);
            for (Map<String, Object> row : normalizeChildRows(childrenPayload.get(relation.modelCode()))) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                validateChildRow(relation, row, true);
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
                childrenChanged = true;
            }
        }

        return childrenChanged;
    }

    public boolean insertJoined(ParentRecord parent, List<RuntimeChildRelation> relations,
                                     Map<String, Map<String, Object>> childDataMap) {
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : relations) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            Object relationValue = relation.mainValue(parent.written(), parent.id(), null);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            repository.insert(relation.tableName(), childData);
            childrenChanged = true;
        }
        return childrenChanged;
    }

    public boolean updateJoined(ParentRecord parent, List<RuntimeChildRelation> relations,
                                     Map<String, Map<String, Object>> childDataMap) {
        Map<String, Object> currentMainRecord = parent.existing();
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : relations) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !parent.written().containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(parent.tableName(), parent.id());
            }
            Object relationValue = relation.mainValue(parent.written(), parent.id(), currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            Long childId = repository.selectFirstIdByColumn(relation.tableName(), relation.childFkColumn(), relationValue);
            if (childId == null) {
                repository.insert(relation.tableName(), childData);
            } else {
                repository.updateById(relation.tableName(), "id", childId, childData, null);
            }
            childrenChanged = true;
        }

        return childrenChanged;
    }

    private boolean mergeMasterDetailChildRows(RuntimeChildRelation relation,
                                               Object relationValue,
                                               List<Map<String, Object>> childRows) {
        boolean changed = false;
        for (Map<String, Object> row : childRows) {
            Object rowId = resolveChildRowId(row);
            if (isDeletedChildRow(row)) {
                changed = ownership.deleteMarkedRow(relation, rowId, relationValue) || changed;
                continue;
            }

            Map<String, Object> childData = filterChildWriteData(row, relation);
            if (rowId == null) {
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                validateChildRow(relation, row, true);
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
                changed = true;
                continue;
            }

            ownership.assertChildRowBelongsToMain(relation, rowId, relationValue);
            if (!hasWritableChildData(childData)) {
                continue;
            }
            validateChildRow(relation, row, false);
            int affected = repository.updateById(relation.tableName(), "id", rowId, childData, null);
            if (affected <= 0) {
                throw new BusinessException("子表行更新失败或数据不存在");
            }
            changed = true;
        }
        return changed;
    }
}
