package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.runtime.service.RuntimeCrudService;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LowcodeTaskPresentationTest {
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final AiCrudConfigVersionMapper versions = mock(AiCrudConfigVersionMapper.class);
    private final RuntimeCrudService crud = mock(RuntimeCrudService.class);
    private final BusinessTaskEngine engine = mock(BusinessTaskEngine.class);
    private final TaskBindingSnapshots snapshots = new TaskBindingSnapshots(new ObjectMapper(), configs, versions);
    private final TaskBindingRuntime runtime = new TaskBindingRuntime(snapshots, crud);
    private final LowcodeTaskPresentation presentation = new LowcodeTaskPresentation(snapshots, runtime, engine);
    private final AiCrudConfig config = new AiCrudConfig();
    private final AiCrudConfigVersion version = new AiCrudConfigVersion();
    private final Map<String, Object> record = new LinkedHashMap<>();
    private final String key = TaskBindingRuntime.key(80, 50, "record-1");

    @BeforeEach
    void setup() {
        String model = """
                {"fields":[{"field":"assignee"},{"field":"title"},{"field":"taskId"},{"field":"state"},
                {"field":"result","label":"检查结论","dataType":"varchar","componentType":"textarea",
                "basicProps":{"onClick":"untrustedScript"}},{"field":"privateNote","dataType":"text"}]}
                """;
        var spec = new TaskBindingSpec(101, "assignee", "title", null, "taskId", "state",
                "OPEN", "DONE", "CANCELLED", Map.of("comment", "result"));
        config.setId(10L);
        config.setTenantId(7L);
        config.setConfigKey("records");
        config.setObjectCode("RECORD");
        config.setPublishStatus("PUBLISHED");
        config.setMode("CONFIG");
        config.setPublishedVersion(2);
        version.setId(80L);
        version.setTenantId(7L);
        version.setConfigId(10L);
        version.setConfigKey("records");
        version.setObjectCode("RECORD");
        version.setVersionNo(2);
        version.setModelSchema(model);
        version.setOptions(snapshots.write(Map.of(TaskBindingSnapshots.OPTION,
                List.of(new PublishedTaskBinding(50, "review", spec)))));
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(versions.selectVersionByNo(7L, 10L, 2)).thenReturn(version);
        when(versions.selectById(80L)).thenReturn(version);
        when(engine.definition(101)).thenReturn(new TaskDefinition(101, "review", 1, "检查任务", "lowcode.record"));
        when(engine.get(200)).thenReturn(task(TaskState.COMPLETED, "{\"comment\":\"符合要求\"}"));
        record.put("taskId", 200L);
        record.put("result", "当前值不作为完成证据");
        record.put("privateNote", "不得泄漏");
        when(crud.selectById("records", "record-1")).thenAnswer(call -> new LinkedHashMap<>(record));
    }

    @AfterEach
    void clear() { TenantContextHolder.clear(); }

    @Test
    void projectsOnlyPublishedMappedFieldsAndCompletedValuesWithoutExecutableProps() {
        var form = as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200));
        assertThat(form.fields()).hasSize(1);
        assertThat(form.fields().get(0)).containsEntry("field", "comment").containsEntry("label", "检查结论")
                .containsEntry("type", "textarea").doesNotContainKey("onClick");
        assertThat(form.fields().get(0).get("props").toString()).doesNotContain("untrustedScript");
        assertThat(form.values()).containsExactlyEntriesOf(Map.of("comment", "符合要求"));
        var actions = as(Set.of("ai:business:RECORD:query"), () -> presentation.actions("records"));
        assertThat(actions).containsExactly(new LowcodeTaskPresentation.Action("review", "检查任务", "taskId"));
    }

    @Test
    void oldTaskUsesItsPublishedFormWhileCurrentVersionStillMustBeCompatible() {
        var latest = new AiCrudConfigVersion();
        org.springframework.beans.BeanUtils.copyProperties(version, latest);
        latest.setId(81L);
        latest.setModelSchema(version.getModelSchema().replace("检查结论", "新版结论"));
        when(versions.selectVersionByNo(7L, 10L, 2)).thenReturn(latest);
        when(engine.get(200)).thenReturn(task(TaskState.IN_PROGRESS, null));
        var form = as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200));
        assertThat(form.fields().get(0)).containsEntry("label", "检查结论");
        assertThat(form.values()).isEmpty();
        latest.setModelSchema(latest.getModelSchema().replace("varchar", "decimal"));
        assertThatThrownBy(() -> as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sourcePermissionScopeTenantAndTaskLinkCannotBeBypassedByPresentation() {
        assertThatThrownBy(() -> as(Set.of(), () -> presentation.form(200))).isInstanceOf(BusinessException.class);
        when(crud.selectById("records", "record-1")).thenReturn(Map.of());
        assertThatThrownBy(() -> as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200)))
                .isInstanceOf(BusinessException.class);
        when(crud.selectById("records", "record-1")).thenReturn(Map.of("taskId", 999));
        assertThatThrownBy(() -> as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200)))
                .isInstanceOf(BusinessException.class);
        version.setTenantId(8L);
        assertThatThrownBy(() -> as(Set.of("ai:business:RECORD:query"), () -> presentation.actions("records")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unsupportedResultComponentsFailClearlyInsteadOfAllowingArbitraryJson() {
        version.setModelSchema(version.getModelSchema().replace("textarea", "fileUpload"));
        assertThatThrownBy(() -> as(Set.of("ai:business:RECORD:query"), () -> presentation.form(200)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("任务");
    }

    private BusinessTask task(TaskState state, String result) {
        return new BusinessTask(200, 101, "lowcode.record", key, "once", 20, "User", "检查任务",
                state, null, null, result, null, 1);
    }

    private <T> T as(Set<String> permissions, Supplier<T> action) {
        LoginUser login = new LoginUser();
        login.setTenantId(7L);
        login.setUserId(20L);
        login.setUserType(2);
        login.setPermissions(permissions);
        TenantContextHolder.setTenantId(7L);
        try (var ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(login, "USER", 20L, null, 1L, "test", "test", Set.of()))) {
            return action.get();
        }
    }
}
