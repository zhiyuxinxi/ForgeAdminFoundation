package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectRecomputeMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds first-release pending recompute tasks for STORED cross-object formulas.
 * <p>
 * The service intentionally keeps the task shape independent from persistence so
 * a durable queue/table can be added without changing formula publish semantics.
 */
@Slf4j
@Service
public class CrossObjectRecomputeTaskService {

    public static final String ALL_RECORDS = "*";
    public static final String STATUS_PENDING = "PENDING";

    private final List<PendingRecomputeTask> pendingTasks = Collections.synchronizedList(new ArrayList<>());

    public List<PendingRecomputeTask> enqueueForPublish(FormulaObjectDependencyAnalyzer.ObjectContext context) {
        List<PendingRecomputeTask> tasks = buildPendingTasks(context, ALL_RECORDS);
        if (!tasks.isEmpty()) {
            pendingTasks.addAll(tasks);
            log.info("Queued cross-object formula recompute tasks: objectCode={}, count={}",
                    context.getObjectCode(), tasks.size());
        }
        return tasks;
    }

    public List<PendingRecomputeTask> buildPendingTasks(FormulaObjectDependencyAnalyzer.ObjectContext context,
                                                        Object recordId) {
        if (context == null || context.getFormulaMap().isEmpty()) {
            return List.of();
        }
        List<PendingRecomputeTask> tasks = new ArrayList<>();
        for (Map.Entry<String, FormulaConfig> entry : context.getFormulaMap().entrySet()) {
            String fieldCode = entry.getKey();
            FormulaConfig formula = entry.getValue();
            if (formula == null || !formula.isStored() || !formula.hasCrossObject()) {
                continue;
            }
            CrossObjectConfig crossObject = formula.getCrossObject();
            String dependencyTrace = buildDependencyTrace(context.getObjectCode(), fieldCode, crossObject);
            tasks.add(new PendingRecomputeTask(
                    context.getObjectCode(),
                    String.valueOf(recordId == null ? ALL_RECORDS : recordId),
                    fieldCode,
                    dependencyTrace,
                    buildIdempotencyKey(context.getObjectCode(), recordId, fieldCode, dependencyTrace),
                    crossObject.getRecomputeMode(),
                    STATUS_PENDING,
                    LocalDateTime.now()));
        }
        return Collections.unmodifiableList(tasks);
    }

    public String buildIdempotencyKey(String objectCode,
                                      Object recordId,
                                      String fieldCode,
                                      String dependencyTrace) {
        return StringUtils.defaultString(objectCode)
                + ":" + String.valueOf(recordId == null ? ALL_RECORDS : recordId)
                + ":" + StringUtils.defaultString(fieldCode)
                + ":" + StringUtils.defaultString(dependencyTrace);
    }

    public List<PendingRecomputeTask> getPendingTasksSnapshot() {
        synchronized (pendingTasks) {
            return List.copyOf(pendingTasks);
        }
    }

    public void clearPendingTasks() {
        pendingTasks.clear();
    }

    private String buildDependencyTrace(String objectCode, String fieldCode, CrossObjectConfig crossObject) {
        return objectCode + "." + fieldCode
                + "->" + crossObject.getTargetObjectCode() + "." + crossObject.getReturnField()
                + "@" + crossObject.getRelationCode();
    }

    public static class PendingRecomputeTask {
        private final String objectCode;
        private final String recordId;
        private final String fieldCode;
        private final String dependencyTrace;
        private final String idempotencyKey;
        private final CrossObjectRecomputeMode recomputeMode;
        private final String status;
        private final LocalDateTime createTime;

        PendingRecomputeTask(String objectCode,
                             String recordId,
                             String fieldCode,
                             String dependencyTrace,
                             String idempotencyKey,
                             CrossObjectRecomputeMode recomputeMode,
                             String status,
                             LocalDateTime createTime) {
            this.objectCode = objectCode;
            this.recordId = recordId;
            this.fieldCode = fieldCode;
            this.dependencyTrace = dependencyTrace;
            this.idempotencyKey = idempotencyKey;
            this.recomputeMode = recomputeMode == null ? CrossObjectRecomputeMode.ASYNC : recomputeMode;
            this.status = status;
            this.createTime = createTime;
        }

        public String getObjectCode() {
            return objectCode;
        }

        public String getRecordId() {
            return recordId;
        }

        public String getFieldCode() {
            return fieldCode;
        }

        public String getDependencyTrace() {
            return dependencyTrace;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public CrossObjectRecomputeMode getRecomputeMode() {
            return recomputeMode;
        }

        public String getStatus() {
            return status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("objectCode", objectCode);
            map.put("recordId", recordId);
            map.put("fieldCode", fieldCode);
            map.put("dependencyTrace", dependencyTrace);
            map.put("idempotencyKey", idempotencyKey);
            map.put("recomputeMode", recomputeMode.name());
            map.put("status", status);
            map.put("createTime", createTime);
            return map;
        }
    }
}
