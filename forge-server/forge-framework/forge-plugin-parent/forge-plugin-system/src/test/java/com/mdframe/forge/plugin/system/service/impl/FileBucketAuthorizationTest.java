package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.plugin.system.mapper.SysFileStorageConfigMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.controller.FileBucketController;
import com.mdframe.forge.starter.file.controller.FileController;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** All storage, metadata and provider effects are mocked; never deletes real resources. */
class FileBucketAuthorizationTest {
    private final FileManager manager = mock(FileManager.class);
    private final FileStorage storage = mock(FileStorage.class);
    private final StorageConfigProvider provider = mock(StorageConfigProvider.class);
    private final ObjectProvider<StorageConfigProvider> beans = mock(ObjectProvider.class);
    private final StorageConfig config = new StorageConfig();
    private final LoginUser caller = new LoginUser();
    private MockedStatic<SessionHelper> session;
    private MockMvc http;

    @BeforeAll
    static void initializeMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "bucket-test"),
                SysFileMetadata.class);
    }

    @BeforeEach
    void setUp() {
        caller.setUserId(100L);
        caller.setTenantId(7L);
        caller.setUserType(0);
        caller.setPermissions(Set.of("*:*:*"));
        session = mockStatic(SessionHelper.class, CALLS_REAL_METHODS);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        config.setId(1L);
        config.setStorageType("local");
        config.setEnabled(true);
        config.setBucketName("approved");
        when(beans.getIfAvailable()).thenReturn(provider);
        when(beans.getObject()).thenReturn(provider);
        when(provider.getConfigByType(anyString())).thenReturn(config);
        when(manager.getStorage(anyString())).thenReturn(storage);
        when(storage.createBucket(anyString())).thenReturn(true);
        when(storage.bucketExists(anyString())).thenReturn(true);
        when(storage.deleteBucket(anyString())).thenReturn(true);
        http = MockMvcBuilders.standaloneSetup(new FileController(manager), new FileBucketController(manager, beans))
                .setControllerAdvice(new ErrorResponse()).build();
    }

    @AfterEach
    void closeIdentity() {
        session.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void tenantAdministratorAndOrdinaryMemberCannotReachAnyOldBucketRoute(int userType) throws Exception {
        caller.setUserType(userType);
        http.perform(post("/api/file/bucket").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isForbidden());
        http.perform(get("/api/file/bucket/exists").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isForbidden());
        http.perform(delete("/api/file/bucket").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(beans, provider, manager, storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "rustfs"})
    void platformAdministratorCanOnlyOperateTheEnabledConfiguredBucket(String type) throws Exception {
        config.setStorageType(type);
        http.perform(post("/api/file/bucket").param("storageType", type).param("bucketName", "approved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        http.perform(get("/api/file/bucket/exists").param("storageType", type).param("bucketName", "approved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        http.perform(delete("/api/file/bucket").param("storageType", type).param("bucketName", "approved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(storage).createBucket("approved");
        verify(storage).bucketExists("approved");
        verify(storage).deleteBucket("approved");
        verifyNoMoreInteractions(storage);
        verify(manager, never()).deleteBucket(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"other-valid", "../approved", "approved/child", ""})
    void administratorCannotSelectANameOutsideTheManagedConfiguration(String bucket) throws Exception {
        http.perform(post("/api/file/bucket").param("storageType", "local").param("bucketName", bucket))
                .andExpect(status().isForbidden());
        http.perform(get("/api/file/bucket/exists").param("storageType", "local").param("bucketName", bucket))
                .andExpect(status().isForbidden());
        http.perform(delete("/api/file/bucket").param("storageType", "local").param("bucketName", bucket))
                .andExpect(status().isForbidden());
        verifyNoInteractions(manager, storage);
    }

    @Test
    void disabledUnmanagedOrMissingConfigurationFailsBeforeStorageCalls() throws Exception {
        config.setEnabled(false);
        assertDeleteForbidden();
        config.setEnabled(true);
        config.setId(null);
        assertDeleteForbidden();
        config.setId(1L);
        config.setStorageType("rustfs");
        assertDeleteForbidden();
        when(beans.getIfAvailable()).thenReturn(null);
        assertDeleteForbidden();
        verifyNoInteractions(manager, storage);
    }

    @Test
    void activeReferencesBlockDeletionAndUnknownProviderDefaultsToDeny() throws Exception {
        when(provider.hasActiveBucketReferences("local", "approved")).thenReturn(true);
        assertDeleteForbidden();
        verifyNoInteractions(storage);
        StorageConfigProvider unknown = mock(StorageConfigProvider.class, CALLS_REAL_METHODS);
        assertThat(unknown.hasActiveBucketReferences("local", "approved")).isTrue();
    }

    @Test
    void failedMutationsReturnFailureWhileAnAbsentBucketRemainsAValidQueryResult() throws Exception {
        when(storage.createBucket("approved")).thenReturn(false);
        when(storage.deleteBucket("approved")).thenReturn(false);
        when(storage.bucketExists("approved")).thenReturn(false);
        http.perform(post("/api/file/bucket").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isInternalServerError());
        http.perform(delete("/api/file/bucket").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isInternalServerError());
        http.perform(get("/api/file/bucket/exists").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "rustfs"})
    void activeReferenceCheckIncludesEveryTenantAndUsesLiteralLocalPrefix(String type) {
        SysFileMetadataMapper metadata = mock(SysFileMetadataMapper.class);
        SystemStorageConfigProvider actual = new SystemStorageConfigProvider(
                mock(SysFileStorageConfigMapper.class), metadata);
        when(metadata.selectCount(any())).thenAnswer(call -> {
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            return 1L;
        });
        assertThat(actual.hasActiveBucketReferences(type, "business_name")).isTrue();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
        ArgumentCaptor<Wrapper<SysFileMetadata>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(metadata).selectCount(query.capture());
        String sql = query.getValue().getSqlSegment();
        assertThat(sql).contains("storage_type", "status", "bucket").doesNotContain("tenant_id", "LIKE");
        if ("local".equals(type)) {
            assertThat(sql).contains("LEFT(REPLACE(file_path, CHAR(92)", "CHAR_LENGTH");
        }
        assertThat(((AbstractWrapper<?, ?, ?>) query.getValue()).getParamNameValuePairs().values())
                .contains(type, 1, "business_name");
        doReturn(0L).when(metadata).selectCount(any());
        assertThat(actual.hasActiveBucketReferences(type, "business_name")).isFalse();
        doReturn(null).when(metadata).selectCount(any());
        assertThat(actual.hasActiveBucketReferences(type, "business_name")).isTrue();
    }

    private void assertDeleteForbidden() throws Exception {
        http.perform(delete("/api/file/bucket").param("storageType", "local").param("bucketName", "approved"))
                .andExpect(status().isForbidden());
    }

    @RestControllerAdvice
    static class ErrorResponse {
        @ExceptionHandler(BusinessException.class)
        ResponseEntity<Map<String, String>> handle(BusinessException error) {
            return ResponseEntity.status(error.getCode()).body(Map.of("message", error.getMessage()));
        }
    }
}
