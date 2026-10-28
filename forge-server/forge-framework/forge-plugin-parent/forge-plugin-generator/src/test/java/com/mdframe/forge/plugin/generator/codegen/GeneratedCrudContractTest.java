package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.*;

/** Exercise the actual downloadable Java artifacts, including annotation processing. */
class GeneratedCrudContractTest {
    @TempDir Path directory;
    private String generatedPrefix;

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "master-detail", "tree"})
    void generatedJavaCompilesAndEnforcesRequestAndResponseBoundaries(String scenario) throws Exception {
        try (URLClassLoader generated = compile(scenario);
             var factory = Validation.buildDefaultValidatorFactory()) {
            String prefix = generatedPrefix;
            Class<?> dtoType = generated.loadClass(prefix + "dto.BizQualityRecordDTO");
            Object dto = dtoType.getConstructor().newInstance();
            Class<?> create = generated.loadClass(dtoType.getName() + "$Create");
            Class<?> update = generated.loadClass(dtoType.getName() + "$Update");
            var validator = factory.getValidator();
            assertTrue(validator.validate(dto, create).stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("recordName")));
            ReflectionTestUtils.invokeMethod(dto, "setRecordName", "A".repeat(129));
            assertFalse(validator.validate(dto, create).isEmpty());
            ReflectionTestUtils.invokeMethod(dto, "setRecordName", "Valid name");
            assertTrue(validator.validate(dto, create).isEmpty());
            assertTrue(validator.validate(dto, update).stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("id")));
            ReflectionTestUtils.invokeMethod(dto, "setId", 1L);
            assertTrue(validator.validate(dto, update).isEmpty());
            assertFalse(validator.validate(dto, create).isEmpty());
            assertThrows(NoSuchFieldException.class, () -> dtoType.getDeclaredField("delFlag"));

            Class<?> vo = generated.loadClass(prefix + "vo.BizQualityRecordVO");
            assertThrows(NoSuchFieldException.class, () -> vo.getDeclaredField("delFlag"));
            Class<?> controller = generated.loadClass(prefix + "controller.BizQualityRecordController");
            for (var method : controller.getDeclaredMethods()) {
                if (method.isSynthetic()) continue;
                assertNotNull(method.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class), method.getName());
                assertFalse(method.getGenericReturnType().getTypeName().contains(".entity."));
            }
            assertEquals(create, controller.getMethod("add", generated.loadClass(prefix + "dto.BizQualityRecord"
                    + (scenario.equals("master-detail") ? "MasterDetailDTO" : "DTO")))
                    .getParameters()[0].getAnnotation(org.springframework.validation.annotation.Validated.class).value()[0]);
            if (scenario.equals("master-detail")) {
                Object aggregate = generated.loadClass(prefix + "dto.BizQualityRecordMasterDetailDTO").getConstructor().newInstance();
                assertFalse(validator.validate(aggregate, create).isEmpty());
                ReflectionTestUtils.invokeMethod(aggregate, "setMain", dto);
                assertFalse(validator.validate(aggregate, create).isEmpty(), "Nested create must reject a supplied generated ID");
                verifyMissingParentDoesNotWriteChildren(generated, aggregate);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verifyMissingParentDoesNotWriteChildren(ClassLoader loader, Object aggregate) throws Exception {
        String prefix = generatedPrefix;
        Class mapperType = loader.loadClass(prefix + "mapper.BizQualityRecordMapper");
        Object mapper = mock(mapperType);
        Class childType = loader.loadClass(prefix + "mapper.BizQualityDetailMapper");
        Object childMapper = mock(childType);
        Object service = org.objenesis.ObjenesisHelper.newInstance(loader.loadClass(prefix + "service.impl.BizQualityRecordServiceImpl"));
        ReflectionTestUtils.setField(service, "bizQualityRecordMapper", mapper);
        ReflectionTestUtils.setField(service, "bizQualityDetailMapper", childMapper);
        var extensions = mock(org.springframework.beans.factory.ObjectProvider.class);
        when(extensions.orderedStream()).thenAnswer(ignored -> java.util.stream.Stream.empty());
        ReflectionTestUtils.setField(service, "extensionProvider", extensions);
        ReflectionTestUtils.invokeMethod(aggregate, "setChildren", Map.of("quality_detail", List.of(Map.of("detailName", "new"))));
        assertEquals(false, ReflectionTestUtils.invokeMethod(service, "updateBizQualityRecord", aggregate));
        verifyNoInteractions(childMapper);
        assertTrue(mockingDetails(mapper).getInvocations().stream()
                .allMatch(invocation -> invocation.getMethod().getName().equals("updateById")));
    }

    private URLClassLoader compile(String scenario) throws Exception {
        var json = new ObjectMapper();
        var config = VelocityCodegenStrategyRefactorTest.fixture(scenario);
        var schema = json.readTree(config.getModelSchema());
        for (var field : schema.path("fields")) {
            if (field.path("field").asText().equals("recordName")) {
                ((ObjectNode) field).put("required", true);
            }
        }
        config.setModelSchema(json.writeValueAsString(schema));
        Map<String, String> artifacts = VelocityCodegenStrategyRefactorTest.strategy(
                VelocityCodegenStrategyRefactorTest.contributor()).generate(config, null);
        String dtoSource = artifacts.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("/dto/BizQualityRecordDTO.java"))
                .findFirst().orElseThrow().getValue();
        generatedPrefix = dtoSource.substring("package ".length(), dtoSource.indexOf(".dto;")) + ".";
        List<Path> javaFiles = new ArrayList<>();
        for (var artifact : artifacts.entrySet()) {
            if (artifact.getKey().endsWith(".java")) {
                Path file = directory.resolve(artifact.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, artifact.getValue(), StandardCharsets.UTF_8);
                javaFiles.add(file);
            }
        }
        Path classes = Files.createDirectories(directory.resolve("classes"));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK is required for generated-code verification");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            boolean compiled = compiler.getTask(null, files, diagnostics,
                    List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString(), "-encoding", "UTF-8"),
                    null, files.getJavaFileObjectsFromPaths(javaFiles)).call();
            assertTrue(compiled, () -> diagnostics.getDiagnostics().toString());
        }
        return new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }
}
