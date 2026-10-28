package com.mdframe.forge.plugin.task.service;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

/** Existing Quartz registration/handler routes call this same bounded entry. No own timer. */
@Component
@JobHandler(value = "publicPeriodicTask", group = "BUSINESS_TASK", description = "Public periodic business tasks")
public class PeriodicTaskJob implements IJobExecutor {
    private final TaskScheduleStore store;
    private final TaskPeriodGenerator generator;
    private final IUserLoadService users;
    private final TransactionTemplate transactions;

    public PeriodicTaskJob(TaskScheduleStore store, TaskPeriodGenerator generator,
                           IUserLoadService users, PlatformTransactionManager manager) {
        this.store = store;
        this.generator = generator;
        this.users = users;
        transactions = new TransactionTemplate(manager);
    }

    @ScheduledJob(name = "publicPeriodicTaskScan", group = "BUSINESS_TASK", cron = "0 0/5 * * * ?",
            description = "Public periodic business tasks")
    public String scan() {
        return execute("");
    }

    @Override
    public String execute(String param) {
        if (param != null && !param.isBlank()) {
            throw TaskMessages.error(400, "period.job.parameters");
        }
        Long previousTenant = TenantContextHolder.getTenantId();
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        boolean previousScope = DataScopeContextHolder.isSkip();
        DynamicDataSourceContextHolder.push("master");
        TenantContextHolder.setIgnore(false);
        DataScopeContextHolder.clearSkip();
        try (var ignored = ExecutionIdentityContextHolder.suspend()) {
            return scanDue();
        } finally {
            if (previousTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previousTenant);
            }
            TenantContextHolder.setIgnore(previousIgnore);
            if (previousScope) {
                DataScopeContextHolder.skipDataScope();
            }
            DynamicDataSourceContextHolder.poll();
        }
    }

    private String scanDue() {
        int generated = 0;
        int failures = 0;
        for (TaskScheduleRow seed : store.due(LocalDateTime.now(ZoneOffset.UTC))) {
            TenantContextHolder.setTenantId(seed.getTenantId());
            try {
                LoginUser user = users.loadUserByUserId(seed.getActorId(), seed.getTenantId(), seed.getActorOrgId());
                validateIdentity(seed, user);
                ExecutionIdentity identity = new ExecutionIdentity(user, "USER", user.getUserId(), null,
                        seed.getId(), "public-periodic-task", "schedule:" + seed.getId(), Set.of());
                try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
                    generated += generator.generate(seed.getId(), 10).generatedPeriods();
                }
            } catch (RuntimeException exception) {
                failures++;
                recordFailure(seed, exception);
            }
        }
        if (failures > 0) {
            throw TaskMessages.error(503, "period.job.failed");
        }
        return "generatedPeriods=" + generated;
    }

    private void validateIdentity(TaskScheduleRow seed, LoginUser user) {
        if (user == null || !Objects.equals(user.getUserId(), seed.getActorId())
                || !Objects.equals(user.getTenantId(), seed.getTenantId())
                || !Objects.equals(user.getActiveOrgId(), seed.getActorOrgId())
                || !Integer.valueOf(1).equals(user.getUserStatus())) {
            throw TaskMessages.error(403, "period.identity.invalid");
        }
    }

    private void recordFailure(TaskScheduleRow seed, RuntimeException exception) {
        transactions.executeWithoutResult(status -> {
            TaskScheduleRow row = store.get(seed.getTenantId(), seed.getId(), true);
            if (row != null && row.getNextGenerateAt() != null) {
                row.setLastFailure(exception.getClass().getSimpleName());
                row.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
                store.update(row);
            }
        });
    }
}
