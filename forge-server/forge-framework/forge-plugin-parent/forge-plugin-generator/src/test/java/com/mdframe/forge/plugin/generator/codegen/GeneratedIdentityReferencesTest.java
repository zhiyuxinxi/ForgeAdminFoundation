package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.identity.*;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Compiles the real generated controller/service and exercises all three shared reference types. */
class GeneratedIdentityReferencesTest {
    @TempDir Path directory;
    private final ObjectMapper json = new ObjectMapper();
    private String prefix;

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "master-detail"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void generatedCandidatesAndWritesUseTheGeneratedModuleAndTrustedFields(String scenario) throws Exception {
        try (URLClassLoader loader = compile(scenario); var session = mockStatic(SessionHelper.class)) {
            Object service = new org.objenesis.ObjenesisStd(false).newInstance(loader.loadClass(prefix + "service.impl.BizQualityRecordServiceImpl"));
            Class mapperType = loader.loadClass(prefix + "mapper.BizQualityRecordMapper");
            Object mapper = mock(mapperType);
            var organizations = mock(TenantOrganizationDirectory.class);
            var members = mock(TenantMemberDirectory.class);
            var posts = mock(TenantPostDirectory.class);
            var scopes = mock(IDataScopeService.class);
            var rows = mock(DynamicCrudRepository.class);
            var rowScopes = mock(RuntimeDataScopeService.class);
            var context = new DataScopeContext();
            context.setTenantId(2L);
            context.setUserId(9L);
            context.setOrgIds(List.of(10L));
            context.setMinDataScope(3);
            var caller = new LoginUser();
            caller.setTenantId(2L);
            caller.setUserId(9L);
            session.when(SessionHelper::getLoginUser).thenReturn(caller);
            ReflectionTestUtils.setField(service, "identityJson", json);
            ReflectionTestUtils.setField(service, "identityReferences", new RuntimeIdentityReferenceService(json, scopes, organizations, members, posts));
            ReflectionTestUtils.setField(service, "identityRecords", rows);
            ReflectionTestUtils.setField(service, "identityRowScopes", rowScopes);
            ReflectionTestUtils.setField(service, "identityDataScopes", scopes);
            ReflectionTestUtils.setField(service, "bizQualityRecordMapper", mapper);
            ObjectProvider extensions = mock(ObjectProvider.class);
            when(extensions.orderedStream()).thenAnswer(ignored -> java.util.stream.Stream.empty());
            ReflectionTestUtils.setField(service, "extensionProvider", extensions);
            if (scenario.equals("master-detail")) {
                ReflectionTestUtils.setField(service, "bizQualityDetailMapper", mock(loader.loadClass(prefix + "mapper.BizQualityDetailMapper")));
            }
            LowcodeRuntimeConfig config = ReflectionTestUtils.invokeMethod(service, "referenceConfig");
            String module = config.getReferencePermissionModule();
            assertNotNull(module);
            assertFalse(module.startsWith("ai:business:"));
            assertEquals(2L, config.getTenantId());
            when(scopes.getCurrentUserDataScope(module)).thenReturn(context);
            session.when(() -> SessionHelper.hasPermission(module + ":add")).thenReturn(true);
            session.when(() -> SessionHelper.hasPermission(module + ":edit")).thenReturn(true);
            when(organizations.findActiveOrganizations(anyCollection())).thenReturn(List.of(new TenantOrganization(10L, 0L, "Department")));
            when(members.findActiveMembersByOrg(eq(10L), any(), any(), anyInt())).thenReturn(List.of(new TenantMember(101L, "user101", "Person")));
            when(members.findActiveMembers(anyCollection(), eq(10L))).thenReturn(List.of(new TenantMember(101L, "user101", "Person")));
            when(posts.findActivePostsByOrg(eq(10L), any(), any(), anyInt())).thenReturn(List.of(new TenantPost(301L, 10L, "role301", "Role")));
            when(posts.findActivePosts(anyCollection(), isNull())).thenReturn(List.of(new TenantPost(301L, 10L, "role301", "Role")));

            assertEquals(1, ((List<?>) ReflectionTestUtils.invokeMethod(service, "referenceOrganizations", "orgId", "add", null, null, 20)).size());
            assertEquals(1, ((List<?>) ReflectionTestUtils.invokeMethod(service, "referenceMembers", "ownerId", "add", 10L, null, null, 20)).size());
            assertEquals(1, ((List<?>) ReflectionTestUtils.invokeMethod(service, "referencePosts", "postId", "add", 10L, false, null, null, 20)).size());
            assertFalse(((RuntimeIdentityReferenceService.PostScope) ReflectionTestUtils.invokeMethod(service, "referencePostScope", "postId", "add")).tenantWideAllowed());
            assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(service, "referenceOrganizations", "invented", "add", null, null, 20));
            assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(service, "referencePosts", "postId", "add", null, true, null, null, 20));
            session.when(() -> SessionHelper.hasPermission(module + ":edit")).thenReturn(false);
            assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(service, "referenceMembers", "ownerId", "edit", 10L, null, null, 20));
            session.when(() -> SessionHelper.hasPermission(module + ":edit")).thenReturn(true);

            var condition = new DynamicCrudRepository.SqlCondition("org_id=:org", Map.of("org", 10L));
            when(rowScopes.buildRuntimeCondition(any(), eq("biz_quality_record"), isNull(), same(context))).thenReturn(condition);
            when(rows.selectById("biz_quality_record", "id", 1L, condition))
                    .thenReturn(Map.of("id", 1L, "org_id", 10L, "owner_id", 101L, "post_id", 301L));
            com.baomidou.mybatisplus.core.mapper.BaseMapper entityMapper = (com.baomidou.mybatisplus.core.mapper.BaseMapper) mapper;
            when(entityMapper.insert(org.mockito.ArgumentMatchers.<Object>any())).thenAnswer(call -> {
                ReflectionTestUtils.invokeMethod((Object) call.getArgument(0), "setId", 1L);
                return 1;
            });
            when(entityMapper.updateById(org.mockito.ArgumentMatchers.<Object>any())).thenReturn(1);
            Object dto = dto(loader, 10L, 101L, 301L);
            assertEquals(true, ReflectionTestUtils.invokeMethod(service, "insertBizQualityRecord", payload(loader, scenario, dto)));
            ReflectionTestUtils.invokeMethod(dto, "setId", 1L);
            assertEquals(true, ReflectionTestUtils.invokeMethod(service, "updateBizQualityRecord", payload(loader, scenario, dto)));
            verify(rows).selectById("biz_quality_record", "id", 1L, condition);
            clearInvocations(mapper);
            for (String setter : List.of("setOrgId", "setOwnerId", "setPostId")) {
                Object rejected = dto(loader, 10L, 101L, 301L);
                ReflectionTestUtils.invokeMethod(rejected, setter, 999L);
                assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(service, "insertBizQualityRecord", payload(loader, scenario, rejected)));
            }
            verify(entityMapper, never()).insert(org.mockito.ArgumentMatchers.<Object>any());
            when(rows.selectById("biz_quality_record", "id", 1L, condition)).thenReturn(null);
            assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(service, "updateBizQualityRecord", payload(loader, scenario, dto)));
            verify(entityMapper, never()).updateById(org.mockito.ArgumentMatchers.<Object>any());
            session.verify(() -> SessionHelper.hasPermission("system:org:list"), never());
            assertController(loader, service, module);
        }
    }

    private Object dto(ClassLoader loader, Long org, Long owner, Long post) throws Exception {
        Object dto = loader.loadClass(prefix + "dto.BizQualityRecordDTO").getConstructor().newInstance();
        ReflectionTestUtils.invokeMethod(dto, "setRecordName", "Valid");
        ReflectionTestUtils.invokeMethod(dto, "setOrgId", org);
        ReflectionTestUtils.invokeMethod(dto, "setOwnerId", owner);
        ReflectionTestUtils.invokeMethod(dto, "setPostId", post);
        return dto;
    }

    private Object payload(ClassLoader loader, String scenario, Object dto) {
        if (!scenario.equals("master-detail")) { return dto; }
        Object aggregate = new org.objenesis.ObjenesisStd(false).newInstance(uncheckedClass(loader, prefix + "dto.BizQualityRecordMasterDetailDTO"));
        ReflectionTestUtils.invokeMethod(aggregate, "setMain", dto);
        ReflectionTestUtils.invokeMethod(aggregate, "setChildren", Map.of());
        return aggregate;
    }

    private Class<?> uncheckedClass(ClassLoader loader, String name) {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException(error);
        }
    }

    private void assertController(ClassLoader loader, Object service, String module) throws Exception {
        Class<?> controllerType = loader.loadClass(prefix + "controller.BizQualityRecordController");
        Object controller = new org.objenesis.ObjenesisStd(false).newInstance(controllerType);
        ReflectionTestUtils.setField(controller, "bizQualityRecordService", service);
        var response = ReflectionTestUtils.invokeMethod(controller, "referenceOrganizations", "orgId", "add", null, null, 20);
        assertNotNull(response);
        var method = controllerType.getMethod("referenceOrganizations", String.class, String.class, Long.class, Long.class, int.class);
        assertArrayEquals(new String[]{module + ":add", module + ":edit"}, method.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class).value());
        assertEquals(cn.dev33.satoken.annotation.SaMode.OR, method.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class).mode());
    }

    private URLClassLoader compile(String scenario) throws Exception {
        var config = VelocityCodegenStrategyRefactorTest.fixture(scenario);
        var model = json.readTree(config.getModelSchema());
        ArrayNode fields = (ArrayNode) model.path("fields");
        for (var ref : Map.of("orgId", "orgTreeSelect", "ownerId", "userSelect", "postId", "postSelect").entrySet()) {
            ObjectNode field = fields.addObject();
            field.put("field", ref.getKey()).put("columnName", ref.getKey().replace("Id", "_id"))
                    .put("dataType", "bigint").put("componentType", ref.getValue()).put("formVisible", true)
                    .put("label", ref.getKey());
        }
        var page = json.readTree(config.getPageSchema());
        ((ObjectNode) page.path("modelRefs").get(0)).set("fields", fields.deepCopy());
        for (var zone : page.path("zones")) {
            if (zone.path("zoneKey").asText().equals("edit")) {
                ((ArrayNode) zone.path("fieldRefs")).add("orgId").add("ownerId").add("postId");
            }
        }
        config.setModelSchema(json.writeValueAsString(model));
        config.setPageSchema(json.writeValueAsString(page));
        config.setEditSchema(json.writeValueAsString(fields));
        Map<String, String> artifacts = VelocityCodegenStrategyRefactorTest.strategy(VelocityCodegenStrategyRefactorTest.contributor()).generate(config, null);
        String source = artifacts.entrySet().stream().filter(entry -> entry.getKey().endsWith("/dto/BizQualityRecordDTO.java"))
                .findFirst().orElseThrow().getValue();
        prefix = source.substring("package ".length(), source.indexOf(".dto;")) + ".";
        var frontend = artifacts.entrySet().stream().filter(entry -> entry.getKey().endsWith("/runtime-config.json")).findFirst().orElseThrow();
        assertEquals("/quality/record/references", json.readTree(frontend.getValue()).path("identityReferenceApiBase").asText());
        List<Path> javaFiles = new ArrayList<>();
        for (var artifact : artifacts.entrySet()) {
            if (!artifact.getKey().endsWith(".java")) { continue; }
            Path file = directory.resolve(artifact.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, artifact.getValue(), StandardCharsets.UTF_8);
            javaFiles.add(file);
        }
        Path classes = Files.createDirectories(directory.resolve("classes"));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            assertTrue(compiler.getTask(null, files, diagnostics,
                    List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString(), "-encoding", "UTF-8"),
                    null, files.getJavaFileObjectsFromPaths(javaFiles)).call(), () -> diagnostics.getDiagnostics().toString());
        }
        return new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }
}
