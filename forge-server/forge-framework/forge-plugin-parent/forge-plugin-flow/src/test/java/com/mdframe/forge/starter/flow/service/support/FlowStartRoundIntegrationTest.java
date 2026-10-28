package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowStartRoundIntegrationTest {
    private static final Map<String, Object> VARIABLES = Map.of("reviewer", "reviewer");
    private static FlowStartRoundDatabaseFixture db;

    @BeforeEach
    void tenantContext() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @BeforeAll
    static void createDatabase() throws Exception {
        db = new FlowStartRoundDatabaseFixture();
    }

    @AfterAll
    static void closeDatabase() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    void rejectedAndCanceledRestartPreserveHistoryAndBindFirstTask() {
        for (String status : new String[]{"rejected", "canceled"}) {
            String key = UUID.randomUUID().toString();
            String first = start(key, "first", null);
            db.finish(first, status);
            String next = start(key, "second", first);
            assertThat(next).isNotEqualTo(first);
            assertThat(db.mapper.selectByProcessInstanceId(first).getStatus()).isEqualTo(status);
            var second = db.mapper.selectByProcessInstanceId(next);
            assertThat(second.getRoundNo()).isEqualTo(2);
            assertThat(second.getPreviousProcessInstanceId()).isEqualTo(first);
            assertThat(db.mapper.selectByBusinessKeyAndTenantId(1L, key).getProcessInstanceId()).isEqualTo(next);
            assertThat(db.engine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(first).singleResult().getEndTime()).isNotNull();
            assertThat(db.firstTasks.stream().filter(task -> next.equals(task.getProcessInstanceId())))
                    .singleElement().satisfies(task -> {
                        assertThat(task.getTenantId()).isEqualTo(1L);
                        assertThat(task.getBusinessKey()).isEqualTo(key);
                        assertThat(task.getTitle()).isEqualTo(key);
                        assertThat(task.getStartUserId()).isEqualTo("owner");
                    });
            assertThat(db.count(key)).isEqualTo(2);
        }
    }

    @Test
    void sameRoundReplaysEvenAfterItEndsAndPayloadReuseConflicts() {
        String key = UUID.randomUUID().toString();
        String first = start(key, "first", null);
        db.finish(first, "rejected");
        String next = start(key, "second", first);
        assertThat(start(key, "second", first)).isEqualTo(next);
        db.finish(next, "approved");
        assertThat(start(key, "second", first)).isEqualTo(next);
        assertThat(start(key, "first", null)).isEqualTo(first);
        assertThatThrownBy(() -> db.start(db.service(), 1, key, "second", first, "owner",
                Map.of("reviewer", "someone-else"))).hasMessage("FLOW_START_REQUEST_CONFLICT");
        assertThat(db.count(key)).isEqualTo(2);
    }

    @Test
    void onlyCurrentEndedRejectedOrCanceledRoundCanRestart() {
        for (String status : new String[]{"approved", "completed", "terminated", "unknown", "running"}) {
            String key = UUID.randomUUID().toString();
            String first = start(key, "first", null);
            if (!"running".equals(status)) {
                db.finish(first, status);
            }
            assertThatThrownBy(() -> start(key, "second", first))
                    .hasMessage("FLOW_START_PREVIOUS_INSTANCE_NOT_RESTARTABLE");
            assertThat(db.count(key)).isEqualTo(1);
        }
        String key = UUID.randomUUID().toString();
        String first = start(key, "first", null);
        db.finish(first, "rejected");
        start(key, "second", first);
        assertThatThrownBy(() -> start(key, "third", first)).hasMessage("FLOW_START_PREVIOUS_INSTANCE_STALE");
    }

    @Test
    void wrongTenantBusinessAndActorCannotUsePredecessor() {
        String key = UUID.randomUUID().toString();
        String first = start(key, "first", null);
        db.finish(first, "rejected");
        assertThatThrownBy(() -> db.start(db.service(), 2, key, "second", first, "owner", VARIABLES))
                .hasMessage("FLOW_START_PREVIOUS_INSTANCE_MISMATCH");
        assertThatThrownBy(() -> start("other-" + key, "second", first))
                .hasMessage("FLOW_START_PREVIOUS_INSTANCE_MISMATCH");
        assertThatThrownBy(() -> db.start(db.service(), 1, key, "second", first, "other", VARIABLES))
                .hasMessage("FLOW_START_PREVIOUS_IDENTITY_MISMATCH");
        assertThat(db.count(key)).isEqualTo(1);
    }

    @Test
    void databaseLockCoordinatesIndependentServiceInstances() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = start(key, "first", null);
        db.finish(first, "rejected");
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var one = executor.submit(() -> {
                ready.countDown();
                go.await();
                return start(key, "second", first);
            });
            var two = executor.submit(() -> {
                ready.countDown();
                go.await();
                return start(key, "second", first);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(one.get(15, TimeUnit.SECONDS)).isEqualTo(two.get(15, TimeUnit.SECONDS));
            assertThat(db.count(key)).isEqualTo(2);
            assertThatThrownBy(() -> start(key, "different-request", first))
                    .hasMessage("FLOW_START_PREVIOUS_INSTANCE_STALE");
        } finally {
            go.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void databaseUniqueKeysProtectRoundAndRequestWhileTenantsRemainIndependent() {
        String key = UUID.randomUUID().toString();
        start(key, "first", null);
        String insert = "INSERT INTO sys_flow_business(id,tenant_id,business_key,round_no,start_request_id) VALUES(?,?,?,?,?)";
        assertThatThrownBy(() -> db.jdbc.update(insert, "duplicate-round", 1, key, 1, "other"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> db.jdbc.update(insert, "duplicate-request", 1, key, 2, "first"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(db.jdbc.update(insert, "separate-tenant", 2, key, 1, "first")).isEqualTo(1);
    }

    @Test
    void engineStartFailureRollsBackOnlyNewRoundAndLegacyBehaviorRemains() {
        String key = UUID.randomUUID().toString();
        String first = start(key, null, null);
        assertThat(start(key, null, null)).isEqualTo(first);
        db.finish(first, "rejected");
        assertThatThrownBy(() -> start(key, null, null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> db.start(db.service(), 1, key, "retry", first, "owner", Map.of()))
                .isInstanceOf(RuntimeException.class);
        assertThat(db.count(key)).isEqualTo(1);
        assertThat(db.mapper.selectByProcessInstanceId(first).getStatus()).isEqualTo("rejected");
        assertThat(db.engine.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(key).count()).isZero();
        assertThat(start(key, "retry", first)).isNotEqualTo(first);
    }

    private static String start(String key, String request, String previous) {
        return db.start(db.service(), 1, key, request, previous, "owner", VARIABLES);
    }
}
