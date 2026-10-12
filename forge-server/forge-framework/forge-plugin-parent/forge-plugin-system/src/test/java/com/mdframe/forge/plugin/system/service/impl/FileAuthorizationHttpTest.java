package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.controller.SysFileMetadataController;
import com.mdframe.forge.plugin.system.entity.SysFileGroup;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileGroupMapper;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.controller.FileController;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.impl.LocalFileStorage;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Servlet entry-point tests with real file services/local storage and isolated metadata mocks. */
class FileAuthorizationHttpTest {
    @TempDir
    Path directory;
    private final SysFileMetadataMapper metadataMapper = mock(SysFileMetadataMapper.class);
    private final SysFileGroupMapper groupMapper = mock(SysFileGroupMapper.class);
    private final Map<Long, SysFileMetadata> rows = new HashMap<>();
    private final LoginUser caller = new LoginUser();
    private MockedStatic<SessionHelper> session;
    private MockedStatic<StpUtil> permissions;
    private SystemFileMetadataPersistence persistence;
    private MockMvc http;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : List.of(SysFileMetadata.class, SysFileGroup.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "file-test"), entity);
        }
    }

    @BeforeEach
    void setUp() {
        caller.setUserId(100L);
        caller.setTenantId(7L);
        caller.setUserType(2);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        permissions = mockStatic(StpUtil.class);
        when(metadataMapper.selectById(any())).thenAnswer(call ->
                rows.get(Long.valueOf(call.getArgument(0).toString())));
        when(metadataMapper.selectOne(any())).thenAnswer(call -> {
            AbstractWrapper<?, ?, ?> wrapper = call.getArgument(0);
            wrapper.getSqlSegment();
            return rows.values().stream().filter(row ->
                    wrapper.getParamNameValuePairs().containsValue(row.getFileId())).findFirst().orElse(null);
        });
        when(metadataMapper.selectList(any())).thenReturn(List.of());
        when(metadataMapper.selectPage(any(Page.class), any())).thenReturn(new Page<SysFileMetadata>());
        when(metadataMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        persistence = new SystemFileMetadataPersistence(metadataMapper);
        StorageConfig config = new StorageConfig();
        config.setStorageType("local");
        config.setBasePath(directory.toString());
        config.setAllowedTypes("txt");
        LocalFileStorage storage = new LocalFileStorage();
        ReflectionTestUtils.setField(storage, "metadataPersistence", persistence);
        storage.init(config);
        FileManager manager = new FileManager();
        manager.registerStorage(storage);
        ReflectionTestUtils.setField(manager, "metadataPersistence", persistence);
        StorageConfigProvider provider = mock(StorageConfigProvider.class);
        when(provider.getDefaultConfig()).thenReturn(config);
        when(provider.getConfigByType("local")).thenReturn(config);
        ReflectionTestUtils.setField(manager, "configProvider", provider);
        SysFileMetadataServiceImpl service = new SysFileMetadataServiceImpl(manager, groupMapper);
        ReflectionTestUtils.setField(service, "baseMapper", metadataMapper);
        http = MockMvcBuilders.standaloneSetup(new FileController(manager),
                        new SysFileMetadataController(service, groupMapper))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build()))
                .setControllerAdvice(new ErrorResponse()).build();
    }

    @AfterEach
    void closeIdentity() {
        permissions.close();
        session.close();
    }

    @Test
    void ownerCanReadUrlMetadataAndDeletePrivateFileThroughExternalRoutes() throws Exception {
        SysFileMetadata own = file(1L, 100L, 7L, true);
        http.perform(get("/api/file/download/" + own.getFileId())).andExpect(status().isOk())
                .andExpect(content().string("file-1"));
        http.perform(get("/api/file/url/" + own.getFileId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("/api/file/download/" + own.getFileId()));
        http.perform(get("/system/file/metadata/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").value(own.getFileId()));
        http.perform(get("/system/file/metadata/byFileId/" + own.getFileId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").value(own.getFileId()));
        http.perform(delete("/api/file/" + own.getFileId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        assertThat(Files.exists(path(own))).isFalse();
        verify(metadataMapper).incrementDownloadCount(own.getFileId());
        verify(metadataMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void anotherMemberCannotReadSignDeleteOrInspectPrivateFile() throws Exception {
        SysFileMetadata other = file(2L, 200L, 7L, true);
        http.perform(get("/api/file/download/" + other.getFileId())).andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Content-Disposition"));
        http.perform(get("/api/file/url/" + other.getFileId())).andExpect(status().isForbidden());
        http.perform(get("/system/file/metadata/2")).andExpect(status().isForbidden());
        http.perform(get("/system/file/metadata/byFileId/" + other.getFileId())).andExpect(status().isForbidden());
        http.perform(delete("/api/file/" + other.getFileId())).andExpect(status().isForbidden());
        http.perform(delete("/system/file/metadata/delete/" + other.getFileId())).andExpect(status().isForbidden());
        assertThat(Files.readString(path(other))).isEqualTo("file-2");
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
        verify(metadataMapper, never()).incrementDownloadCount(anyString());
        assertThat(persistence.checkPermission(other.getFileId(), 200L)).isFalse();
    }

    @Test
    void publicReadDoesNotGrantDeletionOrMutation() throws Exception {
        SysFileMetadata shared = file(2L, 200L, 7L, false);
        http.perform(get("/api/file/download/" + shared.getFileId())).andExpect(status().isOk());
        http.perform(get("/api/file/url/" + shared.getFileId())).andExpect(status().isOk());
        http.perform(delete("/api/file/" + shared.getFileId())).andExpect(status().isForbidden());
        http.perform(put("/system/file/metadata/rename").param("fileId", shared.getFileId())
                .param("originalName", "changed.txt")).andExpect(status().isForbidden());
        assertThat(Files.exists(path(shared))).isTrue();
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void evenOwnerOrAdministratorCannotCrossTenantBoundary(int userType) throws Exception {
        caller.setUserType(userType);
        SysFileMetadata outside = file(3L, 100L, 99L, false);
        http.perform(get("/api/file/download/" + outside.getFileId())).andExpect(status().isForbidden());
        http.perform(get("/api/file/url/" + outside.getFileId())).andExpect(status().isForbidden());
        http.perform(get("/system/file/metadata/3")).andExpect(status().isForbidden());
        http.perform(delete("/api/file/" + outside.getFileId())).andExpect(status().isForbidden());
        assertThat(Files.exists(path(outside))).isTrue();
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void authorizedAdministratorCanManagePrivateFilesWithinCurrentTenant() throws Exception {
        permissions.when(() -> StpUtil.hasPermission("*:*:*")).thenReturn(true);
        SysFileMetadata other = file(2L, 200L, 7L, true);
        http.perform(get("/api/file/download/" + other.getFileId())).andExpect(status().isOk());
        http.perform(delete("/system/file/metadata/2")).andExpect(status().isOk());
        assertThat(Files.exists(path(other))).isFalse();
    }

    @Test
    void mixedBatchIsRejectedBeforeAnyStorageOrMetadataDeletion() throws Exception {
        SysFileMetadata own = file(1L, 100L, 7L, true);
        SysFileMetadata other = file(2L, 200L, 7L, true);
        http.perform(delete("/system/file/metadata/1,2")).andExpect(status().isForbidden());
        assertThat(Files.exists(path(own))).isTrue();
        assertThat(Files.exists(path(other))).isTrue();
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void authorizedBatchDeletesFilesAndStorageFailureIsReportedWithoutDeletingMetadata() throws Exception {
        SysFileMetadata own = file(1L, 100L, 7L, true);
        SysFileMetadata second = file(2L, 100L, 7L, true);
        http.perform(delete("/system/file/metadata/1,2")).andExpect(status().isOk());
        assertThat(Files.exists(path(own))).isFalse();
        assertThat(Files.exists(path(second))).isFalse();
        clearInvocations(metadataMapper);
        http.perform(delete("/system/file/metadata/1")).andExpect(status().isInternalServerError());
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void metadataUpdateUsesSafeGroupDtoAndCannotRewriteOwnerTenantOrStorage() throws Exception {
        SysFileMetadata own = file(1L, 100L, 7L, true);
        group(9L, 7L);
        http.perform(put("/system/file/metadata").contentType("application/json").content("""
                {"id":1,"groupId":9,"uploaderId":200,"tenantId":99,"fileId":"other",
                 "isPrivate":false,"filePath":"other.txt","storageType":"remote","bucket":"other","status":0}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        ArgumentCaptor<LambdaUpdateWrapper<SysFileMetadata>> updates = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(metadataMapper).update(isNull(), updates.capture());
        assertThat(updates.getValue().getSqlSet()).startsWith("group_id=").doesNotContain(
                "uploader", "tenant_id", "file_path", "file_id", "storage_type", "is_private", "status");
        assertThat(own.getUploaderId()).isEqualTo(100L);
        assertThat(own.getTenantId()).isEqualTo(7L);
        assertThat(own.getIsPrivate()).isTrue();
        assertThat(Files.readString(path(own))).isEqualTo("file-1");
    }

    @Test
    void metadataUpdateRejectsAnotherOwnerAndCrossTenantGroup() throws Exception {
        file(1L, 200L, 7L, true);
        file(2L, 100L, 7L, true);
        group(9L, 99L);
        http.perform(put("/system/file/metadata").contentType("application/json")
                .content("{\"id\":1,\"groupId\":9,\"uploaderId\":100}")).andExpect(status().isForbidden());
        http.perform(put("/system/file/metadata").contentType("application/json")
                .content("{\"id\":2,\"groupId\":9}")).andExpect(status().isForbidden());
        verify(metadataMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void pageAndBusinessListsApplyTheSameVisibilityScopeBeforeQuerying() throws Exception {
        http.perform(get("/system/file/metadata/page")).andExpect(status().isOk());
        http.perform(get("/system/file/metadata/business/common/42")).andExpect(status().isOk());
        ArgumentCaptor<Wrapper<SysFileMetadata>> pageScope = ArgumentCaptor.forClass(Wrapper.class);
        verify(metadataMapper).selectPage(any(), pageScope.capture());
        ArgumentCaptor<Wrapper<SysFileMetadata>> businessScope = ArgumentCaptor.forClass(Wrapper.class);
        verify(metadataMapper).selectList(businessScope.capture());
        for (Wrapper<SysFileMetadata> scope : List.of(pageScope.getValue(), businessScope.getValue())) {
            assertThat(scope.getSqlSegment()).contains("tenant_id", "status", "is_private", " OR ", "uploader_id");
            assertThat(((AbstractWrapper<?, ?, ?>) scope).getParamNameValuePairs().values())
                    .contains(7L, 100L, false, 1);
        }
    }

    @Test
    void uploadBindsTrustedOwnerTenantAndPrivateDefaultForLaterAuthorization() throws Exception {
        http.perform(multipart("/api/file/upload")
                .file(new MockMultipartFile("file", "report.txt", "text/plain", "upload".getBytes()))
                .param("storageType", "local")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploaderId").value(100));
        ArgumentCaptor<SysFileMetadata> saved = ArgumentCaptor.forClass(SysFileMetadata.class);
        verify(metadataMapper).insert(saved.capture());
        assertThat(saved.getValue().getUploaderId()).isEqualTo(100L);
        assertThat(saved.getValue().getTenantId()).isEqualTo(7L);
        assertThat(saved.getValue().getIsPrivate()).isTrue();
        assertThat(Files.exists(directory.resolve(saved.getValue().getFilePath()))).isTrue();
    }

    private SysFileMetadata file(long id, long owner, long tenant, boolean privateFile) throws Exception {
        SysFileMetadata file = new SysFileMetadata();
        file.setId(id);
        file.setFileId("file-" + id);
        file.setFilePath("file-" + id + ".txt");
        file.setOriginalName("file-" + id + ".txt");
        file.setMimeType("text/plain");
        file.setStorageType("local");
        file.setUploaderId(owner);
        file.setTenantId(tenant);
        file.setIsPrivate(privateFile);
        file.setStatus(1);
        rows.put(id, file);
        Files.writeString(path(file), "file-" + id);
        return file;
    }

    private Path path(SysFileMetadata file) {
        return directory.resolve(file.getFilePath());
    }

    private void group(long id, long tenant) {
        SysFileGroup group = new SysFileGroup();
        group.setId(id);
        group.setTenantId(tenant);
        group.setStatus(1);
        when(groupMapper.selectById(id)).thenReturn(group);
    }

    @RestControllerAdvice
    static class ErrorResponse {
        @ExceptionHandler(BusinessException.class)
        ResponseEntity<Map<String, String>> handle(BusinessException error) {
            return ResponseEntity.status(error.getCode() == 403 ? 403 : 500)
                    .body(Map.of("message", error.getMessage()));
        }
    }
}
