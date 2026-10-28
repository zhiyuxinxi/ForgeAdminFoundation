package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.runtime.service.RuntimeCrudService;
import com.mdframe.forge.lowcode.runtime.service.task.RuntimeTaskLinkFields;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBindingService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectService;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingDTO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskSourceHandler;
import com.mdframe.forge.task.api.TaskState;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LowcodeTaskBindingTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final AiCrudConfigVersionMapper versions = mock(AiCrudConfigVersionMapper.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
    private final BusinessTaskEngine engine = mock(BusinessTaskEngine.class);
    private final RuntimeCrudService crud = mock(RuntimeCrudService.class);
    private final TaskBindingSnapshots snapshots = new TaskBindingSnapshots(json, configs, versions);
    private final TaskBindingRuntime runtime = new TaskBindingRuntime(snapshots, crud);
    private final LowcodeTaskSource source = new LowcodeTaskSource(runtime);
    private final TaskBindingAuthoring authoring = new TaskBindingAuthoring(snapshots, objects, bindings, engine);
    private final TaskBindingSpec spec = new TaskBindingSpec(101, "assignee", "title", null, "taskId", "state",
            "OPEN", "DONE", "CANCELLED", Map.of("comment", "result"));
    private final AiCrudConfig config = new AiCrudConfig();
    private final AiCrudConfigVersion version = new AiCrudConfigVersion();
    private final AiBusinessBinding binding = new AiBusinessBinding();
    private final Map<String, Object> record = new LinkedHashMap<>();
    private final TaskDefinition definition = new TaskDefinition(101, "review", 1, "Review", LowcodeTaskSource.SOURCE_TYPE);
    private final String key = TaskBindingRuntime.key(80, 50, "record-1");

    @BeforeEach
    void prepare() {
        String schema = "{\"fields\":[{\"field\":\"assignee\"},{\"field\":\"title\"},{\"field\":\"taskId\"},"
                + "{\"field\":\"state\"},{\"field\":\"result\"}]}";
        config.setId(10L);
        config.setTenantId(7L);
        config.setConfigKey("records");
        config.setObjectCode("RECORD");
        config.setPublishStatus("PUBLISHED");
        config.setMode("CONFIG");
        config.setPublishedVersion(2);
        config.setModelSchema(schema);
        config.setRuntimeTableName("records");
        config.setPrimaryKeyColumn("id");
        config.setOptions("{}");
        version.setId(80L);
        version.setTenantId(7L);
        version.setConfigId(10L);
        version.setConfigKey("records");
        version.setVersionNo(2);
        version.setModelSchema(schema);
        version.setObjectCode("RECORD");
        version.setRuntimeTableName("records");
        version.setPrimaryKeyColumn("id");
        version.setOptions(snapshots.write(Map.of(TaskBindingSnapshots.OPTION, List.of(new PublishedTaskBinding(50, "review", spec)))));
        config.setOptions(version.getOptions());
        AiBusinessObject object = new AiBusinessObject();
        object.setId(30L);
        object.setTenantId(7L);
        object.setConfigKey("records");
        object.setObjectCode("RECORD");
        object.setStatus(1);
        binding.setId(50L);
        binding.setTenantId(7L);
        binding.setTargetType("OBJECT");
        binding.setTargetId(30L);
        binding.setTargetCode("RECORD");
        binding.setBindingType("TASK");
        binding.setBindingKey("review");
        binding.setStatus(1);
        binding.setBindingConfig(snapshots.write(spec));
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(versions.selectVersionByNo(7L, 10L, 2)).thenReturn(version);
        when(versions.selectById(80L)).thenReturn(version);
        when(objects.selectById(30L)).thenReturn(object);
        when(objects.selectByConfigKey(7L, "records")).thenReturn(object);
        when(bindings.selectList(any())).thenReturn(List.of(binding));
        when(engine.definition(101)).thenReturn(definition);
        record.put("id", "record-1");
        record.put("assignee", 20L);
        record.put("title", "Actual record title");
        record.put("state", "OPEN");
        when(crud.selectById("records", "record-1")).thenAnswer(call -> new LinkedHashMap<>(record));
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void saveAndPublishResolveRealObjectDefinitionAndPersistImmutableMapping() {
        as(() -> {
            authoring.validate(binding);
            authoring.publish(config);
            assertThat(snapshots.tree(config.getOptions()).path(TaskBindingSnapshots.OPTION).get(0).path("spec").path("definitionId").asLong()).isEqualTo(101);
            binding.setBindingConfig("{\"definitionId\":999}");
            assertThat(snapshots.binding(version, 50).spec()).isEqualTo(spec);
            assertThatThrownBy(() -> authoring.validate(binding)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void actualBindingSaveEntryRunsTaskValidationBeforePersisting() {
        as(() -> {
            BusinessObjectService objectService = mock(BusinessObjectService.class);
            AiBusinessObject actualObject = objects.selectById(30L);
            when(objectService.requireEntity(30L)).thenReturn(actualObject);
            BusinessBindingService service = new BusinessBindingService(null, objectService, null, null);
            ReflectionTestUtils.setField(service, "baseMapper", bindings);
            ReflectionTestUtils.setField(service, "taskBindings", authoring);
            when(bindings.insert(any(AiBusinessBinding.class))).thenAnswer(call -> {
                AiBusinessBinding stored = call.getArgument(0);
                stored.setId(51L);
                return 1;
            });
            BusinessBindingDTO dto = new BusinessBindingDTO();
            dto.setTargetType("OBJECT");
            dto.setTargetId(30L);
            dto.setTargetCode("RECORD");
            dto.setBindingType("TASK");
            dto.setBindingKey("review");
            dto.setBindingName("Review");
            dto.setBindingConfig(snapshots.write(spec));
            dto.setStatus(1);
            assertThat(service.create(dto)).isEqualTo(51L);
            dto.setTargetCode("OTHER");
            assertThatThrownBy(() -> service.create(dto)).isInstanceOf(BusinessException.class);
            verify(bindings, times(1)).insert(any(AiBusinessBinding.class));
            return null;
        });
    }

    @Test
    void customColumnsAndMasterDetailUsePublishedFieldProjectionAndProtectAllLinkAliases() {
        as(() -> {
            String schema = version.getModelSchema().replace("{\"field\":\"assignee\"}",
                    "{\"field\":\"assignee\",\"columnName\":\"owner_member\"}")
                    .replace("{\"field\":\"taskId\"}", "{\"field\":\"taskId\",\"columnName\":\"task_ref\"}");
            version.setModelSchema(schema);
            config.setModelSchema(schema);
            record.remove("assignee");
            record.put("ownerMember", 20L);
            when(crud.selectById("records", "record-1")).thenAnswer(call -> Map.of("main", new LinkedHashMap<>(record)));
            assertThat(runtime.creation(runtime.read(key, TaskSourceHandler.Operation.CREATE)).assigneeId()).isEqualTo(20);
            LowcodeRuntimeConfig values = new LowcodeRuntimeConfig();
            values.setOptions(version.getOptions());
            values.setModelSchema(schema);
            RuntimeTaskLinkFields.validate(values, Map.of("main", Map.of("taskRef", "200")), Map.of("main", Map.of("taskRef", 200)), json);
            assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(values, Map.of("taskRef", 201), Map.of("main", Map.of("taskRef", 200)), json))
                    .isInstanceOf(BusinessException.class);
            assertThat(RuntimeTaskLinkFields.hasBindings(values, json)).isTrue();
            return null;
        });
    }

    @Test
    void saveRejectsWrongObjectUnpublishedTenantAndUnsupportedDatasource() {
        as(() -> {
            binding.setTargetCode("OTHER");
            assertThatThrownBy(() -> authoring.validate(binding)).isInstanceOf(BusinessException.class);
            binding.setTargetCode("RECORD");
            config.setPublishStatus("DRAFT");
            assertThatThrownBy(() -> authoring.validate(binding)).isInstanceOf(BusinessException.class);
            config.setPublishStatus("PUBLISHED");
            version.setRuntimeDatasourceId(8L);
            assertThatThrownBy(() -> authoring.validate(binding)).isInstanceOf(BusinessException.class);
            version.setRuntimeDatasourceId(null);
            version.setTenantId(8L);
            assertThatThrownBy(() -> authoring.validate(binding)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void genericTaskCreateCannotForgeDefinitionAssigneeOccurrenceOrTitle() {
        as(() -> {
            var expected = runtime.creation(runtime.read(key, TaskSourceHandler.Operation.CREATE));
            var actor = new TaskSourceHandler.Actor(7, 20);
            source.validateCreation(definition, expected, actor);
            assertThatThrownBy(() -> source.validateCreation(definition, new BusinessTaskEngine.Create(
                    101, key, "once", 30, expected.title(), null), actor)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> source.validateCreation(definition, new BusinessTaskEngine.Create(
                    101, key, "random-retry", 20, expected.title(), null), actor)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> source.validateCreation(definition, new BusinessTaskEngine.Create(
                    101, key, "once", 20, "Forged title", null), actor)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> source.validateCreation(new TaskDefinition(999, "other", 1, "Other", "lowcode.record"), expected, actor))
                    .isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void facadeCreatesFromRecordAndWritesLinkBeforeDispatchWithoutClientValues() {
        as(() -> {
            BusinessTask task = task(TaskState.DRAFT, null);
            when(engine.create(any())).thenReturn(task);
            when(engine.dispatch(200)).thenReturn(task(TaskState.ASSIGNED, null));
            var service = new LowcodeTaskService(snapshots, runtime, engine);
            assertThat(service.create("records", "review", "record-1").state()).isEqualTo(TaskState.ASSIGNED);
            var order = inOrder(engine, crud);
            order.verify(engine).create(new BusinessTaskEngine.Create(101, key, "once", 20, "Actual record title", null));
            Map<String, Object> expected = new LinkedHashMap<>();
            expected.put("state", "OPEN");
            expected.put("taskId", null);
            order.verify(crud).updateCommandFields("records", "record-1", Map.of("taskId", 200L), expected);
            order.verify(engine).dispatch(200);
            record.put("taskId", 200L);
            record.put("state", "DONE");
            when(engine.get(200)).thenReturn(task(TaskState.COMPLETED, "{\"comment\":\"done\"}"));
            assertThat(service.create("records", "review", "record-1").state()).isEqualTo(TaskState.COMPLETED);
            verify(engine, times(1)).create(any());
            return null;
        });
    }

    @Test
    void completionUsesOnlyMappedFieldsAndConditionalSourceWriteAndPropagatesFailure() {
        as(() -> {
            record.put("taskId", 200L);
            var task = task(TaskState.COMPLETED, "{\"comment\":\"done\"}");
            var actor = new TaskSourceHandler.Actor(7, 20);
            source.validateCompletion(task, task.resultJson(), actor);
            source.onTerminal(task, actor);
            verify(crud).updateCommandFields("records", "record-1", Map.of("result", "done", "state", "DONE"), Map.of("state", "OPEN", "taskId", 200L));
            assertThatThrownBy(() -> source.validateCompletion(task, "{\"comment\":\"done\",\"tenantId\":8}", actor)).isInstanceOf(BusinessException.class);
            doThrow(new BusinessException("state changed")).when(crud).updateCommandFields(anyString(), any(), anyMap(), anyMap());
            assertThatThrownBy(() -> source.onTerminal(task, actor)).hasMessage("state changed");
            record.put("taskId", 201L);
            assertThatThrownBy(() -> source.validateCompletion(task, task.resultJson(), actor)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void readDenialDesignPreviewAndIdentityMismatchNeverReachTaskCreation() {
        as(() -> {
            when(crud.selectById("records", "record-1")).thenReturn(null);
            var service = new LowcodeTaskService(snapshots, runtime, engine);
            assertThatThrownBy(() -> service.create("records", "review", "record-1")).isInstanceOf(BusinessException.class);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("designPreview", "1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            assertThatThrownBy(() -> snapshots.latest("records")).isInstanceOf(BusinessException.class);
            RequestContextHolder.resetRequestAttributes();
            TenantContextHolder.setTenantId(8L);
            assertThatThrownBy(() -> snapshots.latest("records")).isInstanceOf(BusinessException.class);
            verify(engine, never()).create(any());
            return null;
        });
    }

    @Test
    void normalCrudCannotForgeReplaceOrClearTaskLinkButSameValueCanRoundTrip() {
        LowcodeRuntimeConfig runtimeConfig = new LowcodeRuntimeConfig();
        runtimeConfig.setOptions(version.getOptions());
        assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(runtimeConfig, Map.of("taskId", 200), null, json)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(runtimeConfig, Map.of("main", Map.of("task_id", 201)), Map.of("taskId", 200), json)).isInstanceOf(BusinessException.class);
        Map<String, Object> clear = new LinkedHashMap<>();
        clear.put("taskId", null);
        assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(runtimeConfig, clear, Map.of("taskId", 200), json)).isInstanceOf(BusinessException.class);
        RuntimeTaskLinkFields.validate(runtimeConfig, Map.of("taskId", "200"), Map.of("taskId", 200L), json);
    }

    @Test
    void downloadedConsumerUsesPublishedOwnerEndpointsAndNeverCopiesTaskStorage() {
        as(() -> {
            var contributor = new TaskBindingCodegen(snapshots);
            assertThat(contributor.supports(config)).isTrue();
            Map<String, String> files = new LinkedHashMap<>();
            contributor.contributeFiles(config, new VelocityContext(), files);
            assertThat(files.get("frontend/src/api/public-task/10.js")).contains("request.post(path(bindingKey, recordId))", "/api/task/", "JSON.stringify(result)");
            assertThat(files.get("public-capabilities/task-10.json")).contains("\"publishedVersionId\":80", "lowcode.record");
            return null;
        });
    }

    @Test
    void removingBindingAndRepublishingRetainsProtectionOfHistoricalInstanceField() {
        as(() -> {
            when(bindings.selectList(any())).thenReturn(List.of());
            authoring.publish(config);
            assertThat(snapshots.tree(config.getOptions()).path(TaskBindingSnapshots.OPTION)).isEmpty();
            LowcodeRuntimeConfig values = new LowcodeRuntimeConfig();
            values.setOptions(config.getOptions());
            values.setModelSchema(config.getModelSchema());
            assertThat(RuntimeTaskLinkFields.hasBindings(values, json)).isTrue();
            Map<String, Object> clear = new LinkedHashMap<>();
            clear.put("taskId", null);
            assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(values, clear, Map.of("taskId", 200), json)).isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void rollbackBeforeBindingExistedStillRetainsLaterPublishedInstanceProtection() {
        as(() -> {
            config.setOptions("{}");
            authoring.retainPublishedLinks(config);
            LowcodeRuntimeConfig values = new LowcodeRuntimeConfig();
            values.setOptions(config.getOptions());
            values.setModelSchema(config.getModelSchema());
            assertThat(RuntimeTaskLinkFields.hasBindings(values, json)).isTrue();
            Map<String, Object> clear = new LinkedHashMap<>();
            clear.put("taskId", null);
            assertThatThrownBy(() -> RuntimeTaskLinkFields.validate(values, clear, Map.of("taskId", 200), json))
                    .isInstanceOf(BusinessException.class);
            return null;
        });
    }

    @Test
    void firstPublicationWithZeroVersionDoesNotLoadHistoryOrTrustDraftProtectionMetadata() {
        as(() -> {
            config.setPublishedVersion(0);
            config.setPublishStatus("DRAFT");
            config.setOptions("{\"publicTaskLinkFields\":[\"title\"]}");
            when(bindings.selectList(any())).thenReturn(List.of());
            authoring.publish(config);
            assertThat(snapshots.tree(config.getOptions()).path(TaskBindingSnapshots.LINK_FIELDS)).isEmpty();
            verify(versions, never()).selectVersionByNo(7L, 10L, 0);
            return null;
        });
    }

    @Test
    void authorizedDesignPreviewCannotUseDraftOptionsToBypassTaskLinkWriteProtection() {
        as(() -> {
            AiCrudConfig published = new AiCrudConfig();
            org.springframework.beans.BeanUtils.copyProperties(config, published);
            config.setOptions("{}");
            when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(published);
            when(configs.hasDesignPreviewPermission()).thenReturn(true);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ai/crud/records");
            request.setParameter("designPreview", "1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            var adapter = new RuntimeCrudConfigAdapter(configs);
            assertThatThrownBy(() -> adapter.loadLegacy("records")).isInstanceOf(BusinessException.class);
            request.setMethod("GET");
            assertThat(adapter.loadLegacy("records")).isSameAs(config);
            request.setMethod("POST");
            request.setParameter("designPreview", "0");
            assertThat(adapter.loadLegacy("records")).isSameAs(published);
            return null;
        });
    }

    private BusinessTask task(TaskState state, String result) {
        return new BusinessTask(200, 101, "lowcode.record", key, "once", 20, "User", "Actual record title",
                state, null, null, result, null, 1);
    }

    private <T> T as(Supplier<T> action) {
        LoginUser login = new LoginUser();
        login.setTenantId(7L);
        login.setUserId(20L);
        login.setUserType(2);
        login.setPermissions(Set.of("ai:business:RECORD:query", "ai:business:RECORD:edit"));
        TenantContextHolder.setTenantId(7L);
        try (var ignored = ExecutionIdentityContextHolder.open(new ExecutionIdentity(login, "USER", 20L, null, 1L, "test", "test", Set.of()))) {
            return action.get();
        }
    }
}
