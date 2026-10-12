package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileUploadIdentityTest {
    @ParameterizedTest
    @CsvSource({"true,none", "false,none", "true,user", "false,user", "true,tenant", "false,tenant"})
    void missingIdentityFailsBeforeStorageOrPersistence(boolean stream, String missing) {
        FileStorageRegistry storages = mock(FileStorageRegistry.class);
        FileMetadataPersistence metadata = mock(FileMetadataPersistence.class);
        FileUploadOperations uploads = new FileUploadOperations(storages, null, metadata);
        LoginUser user = user();
        if ("user".equals(missing)) user.setUserId(null);
        if ("tenant".equals(missing)) user.setTenantId(null);
        try (MockedStatic<SessionHelper> identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getLoginUser).thenReturn("none".equals(missing) ? null : user);
            assertThatThrownBy(() -> {
                if (stream) uploads.uploadStream(new ByteArrayInputStream(new byte[]{1}), "file.txt",
                        "text/plain", "invoice", "42", "local", true, 1L);
                else uploads.uploadMultipart(new MockMultipartFile("file", "file.txt", "text/plain",
                        new byte[]{1}), "invoice", "42", "local", true);
            }).isInstanceOf(BusinessException.class);
            verifyNoInteractions(storages, metadata);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {100L, 200L})
    void privateDeduplicationRequiresOriginalUploaderEvenForAdministrator(long owner) {
        FileStorageRegistry storages = mock(FileStorageRegistry.class);
        FileStorage storage = mock(FileStorage.class);
        FileMetadataPersistence metadata = mock(FileMetadataPersistence.class);
        StorageConfigProvider configs = mock(StorageConfigProvider.class);
        StorageConfig config = new StorageConfig();
        config.setStorageType("local");
        config.setAllowedTypes("txt");
        config.setMaxFileSize(1);
        when(configs.getConfigByType("local")).thenReturn(config);
        FileUploadOperations uploads = new FileUploadOperations(storages, configs, metadata);
        FileMetadata existing = FileMetadata.builder().businessType("invoice").businessId("42")
                .isPrivate(true).uploaderId(owner).build();
        FileMetadata created = FileMetadata.builder().uploaderId(999L).build();
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1});
        when(metadata.getByMd5(anyString())).thenReturn(existing);
        when(storages.get("local")).thenReturn(storage);
        when(storage.upload(file, "invoice", "42")).thenReturn(created);
        LoginUser user = user();
        user.setUserType(0);
        try (MockedStatic<SessionHelper> identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getLoginUser).thenReturn(user);
            FileMetadata result = uploads.uploadMultipart(file, "invoice", "42", "local", true);
            if (owner == 100L) {
                assertThat(result).isSameAs(existing);
                verifyNoInteractions(storage);
                verify(metadata, never()).save(any());
            } else {
                assertThat(result).isSameAs(created);
                assertThat(created.getUploaderId()).isEqualTo(100L);
                verify(metadata).save(created);
            }
        }
    }

    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setUserId(100L);
        user.setTenantId(7L);
        return user;
    }
}
