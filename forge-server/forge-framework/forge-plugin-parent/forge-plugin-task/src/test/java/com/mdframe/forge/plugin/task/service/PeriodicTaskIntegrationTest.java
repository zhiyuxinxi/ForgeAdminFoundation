package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine.OwnerType;
import com.mdframe.forge.task.api.TaskState;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PeriodicTaskIntegrationTest extends PeriodicTaskFixture {
    @Test
    void savedVersionsAreImmutableDisabledByDefaultAndOnlyOneVersionCanBeEnabled() {
        as(7, 10, () -> {
            var first = create("monthly", today(), 1, true, OwnerType.PERSON);
            assertThat(first.enabled()).isFalse();
            assertThat(schedules.define(first.rule()).id()).isEqualTo(first.id());
            var rule = first.rule();
            var changed = new PeriodicTaskEngine.Define(rule.code(), 1, rule.definitionId(), rule.sourceKey(),
                    "Different", rule.calendar(), rule.owner(), rule.allowLate());
            assertThatThrownBy(() -> schedules.define(changed)).isInstanceOf(BusinessException.class);
            var secondRule = new PeriodicTaskEngine.Define(rule.code(), 2, rule.definitionId(), rule.sourceKey(),
                    rule.title(), rule.calendar(), rule.owner(), rule.allowLate());
            var second = schedules.define(secondRule);
            assertThatThrownBy(() -> schedules.enable(second.id(), true)).isInstanceOf(BusinessException.class);
            schedules.enable(first.id(), false);
            assertThat(schedules.enable(second.id(), true).enabled()).isTrue();
            return null;
        });
    }

    @Test
    void generationBackfillsBoundedPeriodsAndGenericCompletionUsesTheActualPublicSource() {
        var schedule = as(7, 10, () -> create("daily", today().minusDays(2), 3, true, OwnerType.PERSON));
        as(7, 10, () -> {
            assertThat(schedules.generate(schedule.id(), 1).generatedPeriods()).isEqualTo(1);
            assertThat(schedules.generate(schedule.id(), 60).generatedPeriods()).isEqualTo(2);
            assertThat(schedules.generate(schedule.id(), 60).generatedPeriods()).isZero();
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isEqualTo(3);
        as(7, 20, () -> {
            var assigned = engine.assigned(null, null, 10);
            assertThat(assigned).hasSize(3).allMatch(task -> task.state() == TaskState.ASSIGNED);
            long id = assigned.get(0).id();
            engine.start(id);
            assertThatThrownBy(() -> engine.complete(id, new BusinessTaskEngine.Complete("key", "{\"fake\":1}")))
                    .isInstanceOf(BusinessException.class);
            assertThat(engine.complete(id, new BusinessTaskEngine.Complete("key", "{}\n")).state()).isEqualTo(TaskState.COMPLETED);
            assertThat(engine.complete(id, new BusinessTaskEngine.Complete("key", "{}")).id()).isEqualTo(id);
            return null;
        });
        as(8, 10, () -> {
            assertThatThrownBy(() -> schedules.get(schedule.id())).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void dispatchFailureRollsBackEveryTaskAndRetryUsesTheOriginalAllocationSnapshot() {
        selected = List.of(new TenantMember(20L, "u20", "Original 20"), new TenantMember(30L, "u30", "Original 30"));
        var schedule = as(7, 10, () -> create("department", today(), 1, true, OwnerType.DEPARTMENT));
        AtomicInteger dispatches = new AtomicInteger();
        doAnswer(call -> {
            if (dispatches.incrementAndGet() == 2) {
                throw new IllegalStateException("test dispatch rollback");
            }
            return call.callRealMethod();
        }).when(emissions).dispatch(anyLong());
        as(7, 10, () -> {
            assertThatThrownBy(() -> schedules.generate(schedule.id(), 1)).isInstanceOf(IllegalStateException.class);
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT state FROM sys_task_occurrence", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT next_index FROM sys_task_schedule", Integer.class)).isZero();
        selected = List.of(new TenantMember(10L, "new", "Changed responsibility"));
        doCallRealMethod().when(emissions).dispatch(anyLong());
        as(7, 10, () -> schedules.generate(schedule.id(), 1));
        assertThat(jdbc.queryForList("SELECT assignee_id FROM sys_business_task ORDER BY assignee_id", Long.class))
                .containsExactly(20L, 30L);
        assertThat(jdbc.queryForObject("SELECT assignment_json FROM sys_task_occurrence", String.class))
                .contains("Original 20", "Original 30").doesNotContain("Changed responsibility");
    }

    @Test
    void concurrentTicksCannotDuplicateTheSameObligation() throws Exception {
        var schedule = as(7, 10, () -> create("concurrent", today(), 1, true, OwnerType.POST));
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = pool.submit(() -> {
                start.await();
                return as(7, 10, () -> schedules.generate(schedule.id(), 1));
            });
            var second = pool.submit(() -> {
                start.await();
                return as(7, 10, () -> schedules.generate(schedule.id(), 1));
            });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).generatedPeriods()
                    + second.get(10, TimeUnit.SECONDS).generatedPeriods()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_task_occurrence_assignment", Integer.class)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void earlyStartLateCompletionAndDirectCreateCannotBypassPersistedPeriodConstraints() {
        var future = as(7, 10, () -> create("future", today().plusDays(1), 1, false, OwnerType.PERSON));
        as(7, 10, () -> schedules.generate(future.id(), 1));
        long taskId = onlyTask();
        as(7, 20, () -> {
            assertThatThrownBy(() -> engine.start(taskId)).isInstanceOf(BusinessException.class);
            return null;
        });
        jdbc.update("UPDATE sys_task_occurrence SET starts_at=?,due_at=?",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(2), LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        as(7, 20, () -> {
            engine.start(taskId);
            assertThatThrownBy(() -> engine.complete(taskId, new BusinessTaskEngine.Complete("complete", "{}")))
                    .isInstanceOf(BusinessException.class);
            return null;
        });
        as(7, 10, () -> {
            var task = engine.get(taskId);
            assertThatThrownBy(() -> engine.create(new BusinessTaskEngine.Create(task.definitionId(), task.sourceKey(),
                    task.occurrenceKey(), task.assigneeId(), task.title(), task.dueAt()))).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void realJobEntryReloadsPermissionsRejectsForgedParamsAndRestoresTheCallerContext() {
        var schedule = as(7, 10, () -> create("job", today(), 1, true, OwnerType.PERSON));
        assertThatThrownBy(() -> job.execute("{\"tenantId\":7,\"userId\":10}"))
                .isInstanceOf(BusinessException.class);
        as(8, 30, () -> {
            TenantContextHolder.setIgnore(true);
            DataScopeContextHolder.skipDataScope();
            try {
                assertThat(job.execute("")).isEqualTo("generatedPeriods=1");
                assertThat(TenantContextHolder.getTenantId()).isEqualTo(8L);
                assertThat(TenantContextHolder.isIgnore()).isTrue();
                assertThat(DataScopeContextHolder.isSkip()).isTrue();
                assertThat(ExecutionIdentityContextHolder.current().orElseThrow().actorUserId()).isEqualTo(30);
            } finally {
                TenantContextHolder.clearIgnore();
                DataScopeContextHolder.clearSkip();
            }
            return null;
        });
        verify(users).loadUserByUserId(10L, 7L, 70L);
        as(7, 10, () -> {
            assertThat(schedules.get(schedule.id()).nextIndex()).isEqualTo(1);
            return null;
        });
    }

    @Test
    void revokedPublisherDoesNotGenerateOrAdvanceAndCanResumeAfterAuthorizedRepair() {
        var schedule = as(7, 10, () -> create("revoked", today(), 1, true, OwnerType.PERSON));
        var revoked = login(7, 10);
        revoked.setPermissions(java.util.Set.of());
        when(users.loadUserByUserId(10L, 7L, 70L)).thenReturn(revoked);
        assertThatThrownBy(() -> job.scan()).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT next_index FROM sys_task_schedule", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT last_failure FROM sys_task_schedule", String.class)).isEqualTo("BusinessException");
        when(users.loadUserByUserId(10L, 7L, 70L)).thenReturn(login(7, 10));
        as(7, 10, () -> schedules.enable(schedule.id(), true));
        assertThat(job.scan()).isEqualTo("generatedPeriods=1");
    }

    @Test
    void assignmentOverflowAndDisabledRulesDoNotProducePartialTaskLists() {
        var schedule = as(7, 10, () -> create("bounded", today(), 1, true, OwnerType.POST));
        selected = java.util.stream.LongStream.rangeClosed(1, 200)
                .mapToObj(id -> new TenantMember(id, "u" + id, "U" + id)).toList();
        when(directory.findActiveMembersByPost(70L, 200L, 1))
                .thenReturn(List.of(new TenantMember(201L, "extra", "Extra")));
        as(7, 10, () -> {
            assertThatThrownBy(() -> schedules.generate(schedule.id(), 1)).isInstanceOf(BusinessException.class);
            schedules.enable(schedule.id(), false);
            assertThat(schedules.generate(schedule.id(), 1).generatedPeriods()).isZero();
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_task_occurrence", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isZero();
    }

    @Test
    void disabledOrMismatchedPublisherIsRejectedAndAnotherAuthorizedManagerCanStopRetries() {
        var schedule = as(7, 10, () -> create("identity", today(), 1, true, OwnerType.PERSON));
        var disabled = login(7, 10);
        disabled.setUserStatus(0);
        var wrongTenant = login(8, 10);
        var wrongOrg = login(7, 10);
        wrongOrg.setActiveOrgId(71L);
        for (var rejected : List.of(disabled, wrongTenant, wrongOrg)) {
            jdbc.update("UPDATE sys_task_schedule SET next_attempt_at=next_generate_at");
            when(users.loadUserByUserId(10L, 7L, 70L)).thenReturn(rejected);
            assertThatThrownBy(() -> job.scan()).isInstanceOf(BusinessException.class);
            assertThat(jdbc.queryForObject("SELECT next_index FROM sys_task_schedule", Integer.class)).isZero();
        }
        var manager = login(7, 30);
        manager.setPermissions(java.util.Set.of("task:schedule:write"));
        var identity = new com.mdframe.forge.starter.core.context.ExecutionIdentity(manager, "USER", 30L,
                null, 1L, "test", "test", java.util.Set.of());
        TenantContextHolder.executeWithTenant(7L, () -> {
            try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
                assertThatThrownBy(() -> schedules.enable(schedule.id(), true)).isInstanceOf(BusinessException.class);
                assertThat(schedules.enable(schedule.id(), false).enabled()).isFalse();
            }
        });
        assertThat(job.scan()).isEqualTo("generatedPeriods=0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_business_task", Integer.class)).isZero();
    }
}
