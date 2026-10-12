package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionStepDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessActionStepResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ForeachActionStepExecutor")
class BusinessActionForeachStepExecutorTest {

    @Test
    @DisplayName("returns success when collection path is missing")
    void returnsSuccessWhenCollectionPathIsMissing() {
        Fixture fixture = new Fixture(new CaptureStepExecutor());
        BusinessActionExecutionContext context = context(Map.of());

        BusinessActionStepResultVO result = fixture.foreach.execute(context, foreachStep("formData.items", childStep()));

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(0, result.getResult().get("itemCount"));
        assertEquals(List.of(), result.getResult().get("rowResults"));
    }

    @Test
    @DisplayName("executes child steps for every collection item")
    void executesChildStepsForEveryCollectionItem() {
        CaptureStepExecutor childExecutor = new CaptureStepExecutor();
        Fixture fixture = new Fixture(childExecutor);
        BusinessActionExecutionContext context = context(Map.of("items", List.of(
                Map.of("quantity", 3, "itemCode", "A"),
                Map.of("quantity", 5, "itemCode", "B")
        )));
        Map<String, Object> originalScope = new LinkedHashMap<>();
        originalScope.put("requestId", "R1");
        context.setScopedVariables(originalScope);

        BusinessActionStepResultVO result = fixture.foreach.execute(context, foreachStep("formData.items", childStep()));

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(2, result.getResult().get("itemCount"));
        assertEquals(List.of(
                Map.of("quantity", 3, "index", 0, "itemCode", "A"),
                Map.of("quantity", 5, "index", 1, "itemCode", "B")
        ), childExecutor.capturedData);
        assertEquals(originalScope, context.getScopedVariables());
    }

    @Test
    @DisplayName("supports custom item and index aliases")
    void supportsCustomItemAndIndexAliases() {
        CaptureStepExecutor childExecutor = new CaptureStepExecutor();
        Fixture fixture = new Fixture(childExecutor);
        BusinessActionExecutionContext context = context(Map.of("rows", List.of(Map.of("amount", 9))));
        BusinessActionStepDTO step = foreachStep("formData.rows", customAliasChildStep());
        step.getStepConfig().put("itemAlias", "line");
        step.getStepConfig().put("indexAlias", "lineIndex");

        fixture.foreach.execute(context, step);

        assertEquals(List.of(Map.of("amount", 9, "lineIndex", 0)), childExecutor.capturedData);
    }

    @Test
    @DisplayName("propagates child step failure")
    void propagatesChildStepFailure() {
        FailingStepExecutor childExecutor = new FailingStepExecutor(1);
        Fixture fixture = new Fixture(childExecutor);
        BusinessActionExecutionContext context = context(Map.of("items", List.of(
                Map.of("quantity", 3),
                Map.of("quantity", 5)
        )));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> fixture.foreach.execute(context, foreachStep("formData.items", childStep())));

        assertTrue(error.getMessage().contains("动作步骤执行失败"));
        assertEquals(2, childExecutor.executeCount);
    }

    @Test
    @DisplayName("rejects foreach nesting deeper than allowed")
    void rejectsForeachNestingDeeperThanAllowed() {
        Fixture fixture = new Fixture(new CaptureStepExecutor());
        BusinessActionExecutionContext context = context(Map.of("items", List.of(Map.of("quantity", 1))));
        context.setScopedVariables(Map.of("__foreachDepth", 2));

        BusinessException error = assertThrows(BusinessException.class,
                () -> fixture.foreach.execute(context, foreachStep("formData.items", childStep())));

        assertTrue(error.getMessage().contains("最多嵌套 2 层"));
    }

    private BusinessActionExecutionContext context(Map<String, Object> formData) {
        BusinessActionExecutionContext context = new BusinessActionExecutionContext();
        context.setFormData(new LinkedHashMap<>(formData));
        return context;
    }

    private BusinessActionStepDTO foreachStep(String collectionPath, Map<String, Object> childStep) {
        BusinessActionStepDTO step = new BusinessActionStepDTO();
        step.setStepCode("foreach_items");
        step.setStepName("循环明细");
        step.setStepType("FOREACH");
        step.setStepConfig(new LinkedHashMap<>(Map.of(
                "collectionPath", collectionPath,
                "steps", List.of(childStep)
        )));
        return step;
    }

    private Map<String, Object> childStep() {
        return Map.of(
                "stepCode", "capture_line",
                "stepName", "采集行数据",
                "stepType", "CAPTURE",
                "stepConfig", Map.of("fieldMapping", List.of(
                        Map.of("targetField", "quantity", "sourceField", "item.quantity"),
                        Map.of("targetField", "index", "sourceField", "index"),
                        Map.of("targetField", "itemCode", "sourceField", "item.itemCode")
                ))
        );
    }

    private Map<String, Object> customAliasChildStep() {
        return Map.of(
                "stepCode", "capture_line",
                "stepName", "采集行数据",
                "stepType", "CAPTURE",
                "stepConfig", Map.of("fieldMapping", List.of(
                        Map.of("targetField", "amount", "sourceField", "line.amount"),
                        Map.of("targetField", "lineIndex", "sourceField", "lineIndex")
                ))
        );
    }

    private static class Fixture {

        private final ForeachActionStepExecutor foreach;

        Fixture(BusinessActionStepExecutor childExecutor) {
            TestObjectProvider provider = new TestObjectProvider();
            this.foreach = new ForeachActionStepExecutor(provider);
            BusinessActionExecutionService service = new BusinessActionExecutionService(
                    new ObjectMapper(),
                    null,
                    null,
                    null,
                    new TestTransactionManager(),
                    List.of(foreach, childExecutor));
            provider.setService(service);
        }
    }

    private static class TestObjectProvider implements ObjectProvider<BusinessActionExecutionService> {

        private BusinessActionExecutionService service;

        private void setService(BusinessActionExecutionService service) {
            this.service = service;
        }

        @Override
        public BusinessActionExecutionService getObject(Object... args) throws BeansException {
            return service;
        }

        @Override
        public BusinessActionExecutionService getIfAvailable() throws BeansException {
            return service;
        }

        @Override
        public BusinessActionExecutionService getIfUnique() throws BeansException {
            return service;
        }

        @Override
        public BusinessActionExecutionService getObject() throws BeansException {
            return service;
        }
    }

    private static class CaptureStepExecutor implements BusinessActionStepExecutor {

        private final List<Map<String, Object>> capturedData = new ArrayList<>();

        @Override
        public String supportType() {
            return "CAPTURE";
        }

        @Override
        public BusinessActionStepResultVO execute(BusinessActionExecutionContext context, BusinessActionStepDTO step) {
            Map<String, Object> data = BusinessActionStepConfigHelper.buildData(step.getStepConfig(), context);
            capturedData.add(data);
            BusinessActionStepResultVO result = new BusinessActionStepResultVO();
            result.setStatus("SUCCESS");
            result.setMessage("captured");
            result.getResult().put("data", data);
            return result;
        }
    }

    private static class FailingStepExecutor implements BusinessActionStepExecutor {

        private final int failIndex;
        private int executeCount;

        private FailingStepExecutor(int failIndex) {
            this.failIndex = failIndex;
        }

        @Override
        public String supportType() {
            return "CAPTURE";
        }

        @Override
        public BusinessActionStepResultVO execute(BusinessActionExecutionContext context, BusinessActionStepDTO step) {
            executeCount++;
            Object index = BusinessActionStepConfigHelper.resolvePath("index", context);
            if (index instanceof Number number && number.intValue() == failIndex) {
                throw new BusinessException("行执行失败");
            }
            BusinessActionStepResultVO result = new BusinessActionStepResultVO();
            result.setStatus("SUCCESS");
            result.setMessage("captured");
            return result;
        }
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
