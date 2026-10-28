package com.mdframe.forge.plugin.task.service;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.task.controller.BusinessTaskController;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskState;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real production XML/interceptors and Spring transactions; H2 is not a MySQL migration proof. */
class BusinessTaskEngineIntegrationTest extends TaskEngineFixture {
    @Test
    void lifecycleUsesOneOwnerAndCommitsSourceWriteBackOnce() {
        as(7, 10, () -> {
            BusinessTask created = create("one");
            assertThat(created.state()).isEqualTo(TaskState.DRAFT);
            assertThat(engine.assigned(null, null, 20)).isEmpty();
            assertThat(engine.dispatch(created.id()).state()).isEqualTo(TaskState.ASSIGNED);
            return created.id();
        });
        Long id = jdbc.queryForObject("SELECT id FROM sys_business_task", Long.class);
        as(7, 20, () -> {
            assertThat(engine.assigned(null, null, 20)).extracting(BusinessTask::id).containsExactly(id);
            assertThat(engine.start(id).state()).isEqualTo(TaskState.IN_PROGRESS);
            BusinessTask completed = engine.complete(id, new BusinessTaskEngine.Complete("finish-1", "{\"ok\":true}"));
            assertThat(completed.state()).isEqualTo(TaskState.COMPLETED);
            assertThat(completed.version()).isEqualTo(3);
            assertThat(completed.completedAt()).isNotNull();
            assertThat(engine.complete(id, new BusinessTaskEngine.Complete("finish-1", "{ \"ok\" : true }")).id()).isEqualTo(id);
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("finish-1", "{\"ok\":false}")))
                    .isInstanceOf(BusinessException.class);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT writes FROM task_test_source WHERE tenant_id=7", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT state FROM task_test_source WHERE tenant_id=7", String.class)).isEqualTo("COMPLETED");
    }

    @Test
    void repeatedCreationUsesObligationIdentityAndRejectsChangedContent() {
        as(7, 10, () -> {
            BusinessTask first = create("same-period");
            assertThat(create("same-period").id()).isEqualTo(first.id());
            assertThatThrownBy(() -> engine.create(new BusinessTaskEngine.Create(first.definitionId(),
                    "record-1", "same-period", 20, "Changed title", null))).isInstanceOf(BusinessException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isEqualTo(1);
            assertThat(engine.definitions("test.source", null, 10)).hasSize(1);
            assertThat(engine.definition(first.definitionId()).id()).isEqualTo(first.definitionId());
            assertThatThrownBy(() -> engine.create(new BusinessTaskEngine.Create(first.definitionId(),
                    "record-1", "forged-assignee", 30, "Task", null)))
                    .isInstanceOf(BusinessException.class).hasMessage("source assignee denied");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isEqualTo(1);
            return null;
        });
    }

    @Test
    void terminalCallbackFailureRollsBackTaskAndSourceAndAllowsRetry() {
        long id = preparedTask();
        source.failWriteBack = true;
        as(7, 20, () -> {
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("finish", "{\"ok\":true}")))
                    .isInstanceOf(IllegalStateException.class).hasMessage("source rejected");
            assertThat(engine.get(id).state()).isEqualTo(TaskState.IN_PROGRESS);
            assertThat(engine.get(id).version()).isEqualTo(2);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT writes FROM task_test_source WHERE tenant_id=7", Integer.class)).isZero();
        source.failWriteBack = false;
        as(7, 20, () -> engine.complete(id, new BusinessTaskEngine.Complete("finish", "{\"ok\":true}")));
        assertThat(jdbc.queryForObject("SELECT writes FROM task_test_source WHERE tenant_id=7", Integer.class)).isEqualTo(1);
    }

    @Test
    void outerBusinessRollbackDoesNotLeaveAnAssignedTask() {
        as(7, 10, () -> {
            TransactionTemplate outer = new TransactionTemplate(transactionManager);
            assertThatThrownBy(() -> outer.execute(status -> {
                engine.dispatch(create("rolled-back").id());
                throw new IllegalStateException("business failed");
            })).isInstanceOf(IllegalStateException.class);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isZero();
    }

    @Test
    void tenantSourceAndAssigneeChecksRejectForgedAccess() {
        long id = preparedTask();
        as(8, 20, () -> {
            assertThatThrownBy(() -> engine.get(id)).isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
            return null;
        });
        as(7, 30, () -> {
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "{}")))
                    .isInstanceOf(BusinessException.class).extracting("code").isEqualTo(403);
            assertThat(engine.assigned(null, null, 20)).isEmpty();
            return null;
        });
        as(7, 10, () -> {
            long definition = engine.definitions("test.source", null, 10).get(0).id();
            assertThatThrownBy(() -> engine.create(new BusinessTaskEngine.Create(
                    definition, "missing-source", "one", 20, "Task", null)))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> engine.create(new BusinessTaskEngine.Create(
                    definition, "record-1", "other", 999, "Task", null)))
                    .isInstanceOf(BusinessException.class);
            TenantContextHolder.setTenantId(8L);
            assertThatThrownBy(() -> engine.get(id)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void completionValidatesSourceRulesAndCurrentMemberBeforeMutation() {
        long id = preparedTask();
        as(7, 20, () -> {
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "{\"ok\":false}")))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "[]")))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "{} {}")))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "{\"ok\":true,\"ok\":false}")))
                    .isInstanceOf(BusinessException.class);
            memberEnabled = false;
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("x", "{\"ok\":true}")))
                    .isInstanceOf(BusinessException.class);
            assertThat(engine.get(id).state()).isEqualTo(TaskState.IN_PROGRESS);
            return null;
        });
    }

    @Test
    void concurrentCreationAndCompletionProduceOneTaskAndOneWriteBack() throws Exception {
        long definition = as(7, 10, () -> engine.define(new BusinessTaskEngine.Define("inspection", 1, "Inspection", "test.source")).id());
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Long> create = () -> as(7, 10, () -> engine.create(new BusinessTaskEngine.Create(
                    definition, "record-1", "same", 20, "Task", null)).id());
            var created = pool.invokeAll(List.of(create, create));
            long id = created.get(0).get();
            assertThat(created.get(1).get()).isEqualTo(id);
            as(7, 10, () -> engine.dispatch(id));
            as(7, 20, () -> engine.start(id));
            Callable<TaskState> complete = () -> as(7, 20, () -> engine.complete(
                    id, new BusinessTaskEngine.Complete("finish", "{\"ok\":true}")).state());
            for (var result : pool.invokeAll(List.of(complete, complete))) {
                assertThat(result.get()).isEqualTo(TaskState.COMPLETED);
            }
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT writes FROM task_test_source WHERE tenant_id=7", Integer.class)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cancellationIsTerminalAndIdempotentAndDoesNotMarkCompletion() {
        long id = as(7, 10, () -> create("cancel").id());
        as(7, 10, () -> {
            BusinessTask cancelled = engine.cancel(id, "Source withdrawn");
            assertThat(cancelled.state()).isEqualTo(TaskState.CANCELLED);
            assertThat(cancelled.completedAt()).isNull();
            assertThat(engine.cancel(id, "Source withdrawn").version()).isEqualTo(1);
            assertThatThrownBy(() -> engine.dispatch(id)).isInstanceOf(BusinessException.class);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT writes FROM task_test_source WHERE tenant_id=7", Integer.class)).isEqualTo(1);
    }

    @Test
    void definitionPermissionAndExternalDatasourceAreRejectedAtPublicServiceEntry() {
        as(7, 20, () -> {
            assertThatThrownBy(() -> engine.define(new BusinessTaskEngine.Define("x", 1, "X", "test.source")))
                    .isInstanceOf(BusinessException.class).extracting("code").isEqualTo(403);
            return null;
        });
        as(7, 10, () -> {
            DynamicDataSourceContextHolder.push("external");
            try {
                assertThatThrownBy(() -> create("external")).isInstanceOf(BusinessException.class);
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
            return null;
        });
    }

    @Test
    void readApiUsesActualRouteAndResponseAndPreservesQueryBounds() throws Exception {
        long id = preparedTask();
        var mvc = MockMvcBuilders.standaloneSetup(new BusinessTaskController(engine)).build();
        as(7, 20, () -> {
            try {
                mvc.perform(get("/api/task/{id}", id)).andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.state").value("IN_PROGRESS"))
                        .andExpect(jsonPath("$.data.sourceKey").value("record-1"));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            assertThatThrownBy(() -> engine.assigned(null, null, 201)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    private long preparedTask() {
        long id = as(7, 10, () -> engine.dispatch(create("ready").id()).id());
        as(7, 20, () -> engine.start(id));
        return id;
    }

    private BusinessTask create(String occurrence) {
        long definition = engine.define(new BusinessTaskEngine.Define("inspection", 1, "Inspection", "test.source")).id();
        return engine.create(new BusinessTaskEngine.Create(definition, "record-1", occurrence, 20, "Task", null));
    }

}
