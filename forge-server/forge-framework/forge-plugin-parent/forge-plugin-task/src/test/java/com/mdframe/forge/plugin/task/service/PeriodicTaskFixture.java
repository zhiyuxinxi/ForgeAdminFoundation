package com.mdframe.forge.plugin.task.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.task.mapper.BusinessTaskMapper;
import com.mdframe.forge.plugin.task.mapper.TaskScheduleMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Calendar;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Cycle;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Owner;
import com.mdframe.forge.task.api.PeriodicTaskEngine.OwnerType;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PeriodicTaskFixture extends TaskEngineFixture {
    protected TaskScheduleStore scheduleStore;
    protected TaskScheduleService schedules;
    protected TaskPeriodGenerator generator;
    protected BusinessTaskEngine emissions;
    protected PeriodicTaskJob job;
    protected IUserLoadService users;
    protected TenantMemberDirectory directory;
    protected volatile List<TenantMember> selected = List.of(new TenantMember(20L, "member20", "Snapshot name"));

    @BeforeEach
    void preparePeriodic() throws Exception {
        String ddl = Files.readString(Path.of("../../../db/migration/V1.0.2026101602__public_periodic_business_tasks.sql"))
                .replace(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin", "")
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "");
        for (String statement : ddl.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement);
            }
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TenantProperties tenant = new TenantProperties();
        tenant.setIgnoreTables(List.of());
        tenant.setStrictMode(true);
        tenant.setAutoDetectTenantColumn(false);
        MybatisPlusInterceptor interceptors = new MybatisPlusInterceptor();
        interceptors.addInnerInterceptor(new TenantLineInnerInterceptor(new DefaultTenantLineHandler(tenant)));
        DataScopeProperties scope = new DataScopeProperties();
        scope.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        interceptors.addInnerInterceptor(new DataScopeInterceptor(mock(IDataScopeService.class), scope));
        configuration.addInterceptor(interceptors);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(jdbc.getDataSource());
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/BusinessTaskMapper.xml"),
                new ClassPathResource("mapper/TaskScheduleMapper.xml"));
        SqlSessionTemplate sql = new SqlSessionTemplate(factory.getObject());
        scheduleStore = new TaskScheduleStore(sql.getMapper(TaskScheduleMapper.class));
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        TaskSources sources = new TaskSources(List.of(new PublicPeriodicTaskSource(scheduleStore, json), source));
        TaskPersistence store = new TaskPersistence(sql.getMapper(BusinessTaskMapper.class));
        TaskCatalog catalog = new TaskCatalog(store, sources, new Members(), transactionManager);
        engine = new DefaultBusinessTaskEngine(store, sources, catalog, json, transactionManager,
                new TaskPeriodConstraints(scheduleStore));
        emissions = spy(engine);
        TaskScheduleRules rules = new TaskScheduleRules(json, sources, catalog);
        directory = mock(TenantMemberDirectory.class);
        when(directory.findActiveMembers(anyCollection(), isNull())).thenAnswer(call -> selected);
        when(directory.findActiveMembersByOrg(anyLong(), isNull(), isNull(), eq(200))).thenAnswer(call -> selected);
        when(directory.findActiveMembersByPost(anyLong(), isNull(), eq(200))).thenAnswer(call -> selected);
        generator = new TaskPeriodGenerator(scheduleStore, rules, new TaskPeriodAllocation(directory), emissions, transactionManager);
        schedules = new TaskScheduleService(scheduleStore, rules, generator, transactionManager);
        users = mock(IUserLoadService.class);
        when(users.loadUserByUserId(10L, 7L, 70L)).thenAnswer(call -> login(7, 10));
        job = new PeriodicTaskJob(scheduleStore, generator, users, transactionManager);
    }

    protected LoginUser login(long tenant, long user) {
        LoginUser result = new LoginUser();
        result.setTenantId(tenant);
        result.setUserId(user);
        result.setUserType(2);
        result.setUserStatus(1);
        result.setActiveOrgId(70L);
        result.setPermissions(user == 10 ? Set.of("task:definition:write", "task:schedule:write") : Set.of());
        return result;
    }

    @Override
    protected <T> T as(long tenant, long user, Supplier<T> action) {
        var identity = new ExecutionIdentity(login(tenant, user), "USER", user, null, 1L, "test", "test", Set.of());
        return TenantContextHolder.executeWithTenant(tenant, () -> {
            try (var ignored = ExecutionIdentityContextHolder.open(identity)) {
                return action.get();
            }
        });
    }

    protected PeriodicTaskEngine.Schedule create(String code, LocalDate first, int count, boolean late, OwnerType type) {
        long definition = engine.define(new BusinessTaskEngine.Define("public-confirm", 1, "Confirm", "task.periodic")).id();
        var calendar = new Calendar(Cycle.DAILY, 1, first, "UTC", 3, 1, count, null);
        var rule = new PeriodicTaskEngine.Define(code, 1, definition, "self", "Daily confirmation", calendar,
                new Owner(type, type == OwnerType.PERSON ? 20 : 70), late);
        var saved = schedules.define(rule);
        schedules.enable(saved.id(), true);
        return saved;
    }

    protected LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    protected long onlyTask() {
        return jdbc.queryForObject("SELECT id FROM sys_business_task", Long.class);
    }
}
