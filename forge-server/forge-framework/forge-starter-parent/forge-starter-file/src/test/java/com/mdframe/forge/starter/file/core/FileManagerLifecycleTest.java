package com.mdframe.forge.starter.file.core;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The same contract runs before and after extraction; all storage effects are mocked. */
class FileManagerLifecycleTest {
    private final FileManager manager = new FileManager();
    private final FileStorage storage = mock(FileStorage.class);
    private final FileMetadataPersistence metadata = mock(FileMetadataPersistence.class);
    private final StorageConfigProvider configs = mock(StorageConfigProvider.class);
    private final StorageConfig config = new StorageConfig();
    private MockedStatic<StpUtil> permissions;
    private MockedStatic<SessionHelper> identity;
    private final FileMetadata file = FileMetadata.builder().fileId("file-1").storageType("local")
            .originalName("report.pdf").mimeType("application/pdf").businessType("invoice").businessId("42").build();

    @BeforeEach
    void setUp() {
        permissions = mockStatic(StpUtil.class);
        permissions.when(() -> StpUtil.hasPermission("*:*:*")).thenReturn(true);
        LoginUser caller = new LoginUser();
        caller.setUserId(100L);
        caller.setTenantId(7L);
        identity = mockStatic(SessionHelper.class);
        identity.when(SessionHelper::getLoginUser).thenReturn(caller);
        config.setStorageType("local");
        config.setAllowedTypes(" .PDF,txt ");
        config.setMaxFileSize(1);
        when(storage.getStorageType()).thenReturn("local");
        manager.registerStorage(storage);
        clearInvocations(storage);
        when(configs.getDefaultConfig()).thenReturn(config);
        when(configs.getConfigByType("local")).thenReturn(config);
        when(metadata.getById("file-1")).thenReturn(file);
        ReflectionTestUtils.setField(manager, "configProvider", configs);
        ReflectionTestUtils.setField(manager, "metadataPersistence", metadata);
    }

    @AfterEach
    void closeIdentity() {
        identity.close();
        permissions.close();
    }

    @Test
    void multipartUploadRetainsHashVisibilityAndBusinessScopedDeduplication() {
        MockMultipartFile input = pdf();
        when(storage.upload(input, "invoice", "42")).thenReturn(file);
        assertThat(manager.upload(input, "invoice", "42", null, true)).isSameAs(file);
        assertThat(file.getMd5()).hasSize(32);
        assertThat(file.getIsPrivate()).isTrue();
        verify(metadata).save(file);
        when(metadata.getByMd5(file.getMd5())).thenReturn(file);
        assertThat(manager.upload(input, "invoice", "42", "local")).isSameAs(file);
        verify(storage, times(1)).upload(input, "invoice", "42");
        FileMetadata other = FileMetadata.builder().fileId("file-2").build();
        when(storage.upload(input, "invoice", "43")).thenReturn(other);
        assertThat(manager.upload(input, "invoice", "43", "local", false)).isSameAs(other);
        assertThat(other.getIsPrivate()).isFalse();
        verify(metadata).save(other);
    }

    @Test
    void streamUploadRetainsDefaultStorageKnownSizeAndMetadataSave() {
        ByteArrayInputStream input = content();
        when(storage.upload(input, "report.pdf", "application/pdf", "invoice", "42", 4L)).thenReturn(file);
        assertThat(manager.upload(input, "report.pdf", "application/pdf", "invoice", "42", null, false, 4L))
                .isSameAs(file);
        assertThat(file.getFileSize()).isEqualTo(4L);
        assertThat(file.getIsPrivate()).isFalse();
        verify(metadata).save(file);
        assertThatThrownBy(() -> manager.upload(content(), "report.pdf", "application/pdf", "invoice", "42",
                "local", true, 1048577L)).isInstanceOf(RuntimeException.class);
        verify(storage).upload(input, "report.pdf", "application/pdf", "invoice", "42", 4L);
        verifyNoMoreInteractions(storage);
    }

    @Test
    void multipartUploadRejectsMissingMetadataBeforePersistence() {
        MockMultipartFile input = pdf();
        when(storage.upload(input, "invoice", "42")).thenReturn(null);

        assertThatThrownBy(() -> manager.upload(input, "invoice", "42", "local", true))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UiMessages.message("customer.uploadMetadataMissing"));
        verify(storage).upload(input, "invoice", "42");
        verify(metadata, never()).save(any());
    }

    @Test
    void streamUploadRejectsMissingMetadataBeforePersistence() {
        ByteArrayInputStream input = content();
        when(storage.upload(input, "report.pdf", "application/pdf", "invoice", "42", 4L)).thenReturn(null);

        assertThatThrownBy(() -> manager.upload(input, "report.pdf", "application/pdf", "invoice", "42",
                "local", true, 4L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UiMessages.message("customer.uploadMetadataMissing"));
        verify(storage).upload(input, "report.pdf", "application/pdf", "invoice", "42", 4L);
        verify(metadata, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"report.exe,application/octet-stream", "report.pdf,text/html", "report.pdf,image/png", "report,application/pdf"})
    void invalidUploadPolicyFailsBeforeStorage(String name, String mime) {
        MockMultipartFile input = new MockMultipartFile("file", name, mime, new byte[]{1});
        assertThatThrownBy(() -> manager.upload(input, "invoice", "42", "local"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(storage, metadata);
    }

    @Test
    void multipartLifecycleRetainsOrderArgumentsAndPersistence() {
        when(storage.initMultipartUpload("report.pdf", "invoice", "42")).thenReturn("upload-1");
        String uploadId = manager.initMultipartUpload("report.pdf", "invoice", "42", "local");
        assertThat(uploadId).isNotBlank().isNotEqualTo("upload-1");
        ByteArrayInputStream part = content();
        when(storage.uploadPart(eq("upload-1"), eq(1), any(InputStream.class))).thenAnswer(call -> {
            call.getArgument(2, InputStream.class).readAllBytes();
            return "etag-1";
        });
        assertThat(manager.uploadPart(uploadId, 1, part, "local")).isEqualTo("etag-1");
        List<String> tags = List.of("etag-1");
        when(storage.completeMultipartUpload("upload-1", tags)).thenReturn(file);
        assertThat(manager.completeMultipartUpload(uploadId, tags, "local")).isSameAs(file);
        InOrder order = inOrder(storage, metadata);
        order.verify(storage).initMultipartUpload("report.pdf", "invoice", "42");
        order.verify(storage).uploadPart(eq("upload-1"), eq(1), any(InputStream.class));
        order.verify(storage).completeMultipartUpload("upload-1", tags);
        order.verify(metadata).save(file);
        assertThatThrownBy(() -> manager.initMultipartUpload("bad.exe", "invoice", "42", "local"))
                .isInstanceOf(RuntimeException.class);
        verify(storage, never()).initMultipartUpload("bad.exe", "invoice", "42");
    }

    @Test
    void readAndDeleteAuthorizationRunsBeforeMetadataAndStorageEffects() {
        assertThatThrownBy(() -> manager.download("file-1", new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> manager.getAccessUrl("file-1", 60)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> manager.delete("file-1")).isInstanceOf(BusinessException.class);
        verify(metadata, never()).getById(anyString());
        verifyNoInteractions(storage);
    }

    @Test
    void downloadHeadersBytesCounterAndFailedDeletionRemainConsistent() throws Exception {
        when(metadata.checkPermission("file-1", null)).thenReturn(true);
        when(metadata.checkWritePermission("file-1", null)).thenReturn(true);
        when(storage.download("file-1")).thenReturn(content());
        when(storage.getAccessUrl("file-1", 90)).thenReturn("signed-url");
        MockHttpServletResponse response = new MockHttpServletResponse();
        manager.download("file-1", response);
        assertThat(response.getContentAsByteArray()).containsExactly("data".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment;filename=report.pdf");
        verify(metadata).incrementDownloadCount("file-1");
        assertThat(manager.getAccessUrl("file-1", 90)).isEqualTo("signed-url");
        assertThat(manager.delete("file-1")).isFalse();
        verify(metadata, never()).delete(anyString());
        when(storage.delete("file-1")).thenReturn(true);
        assertThat(manager.delete("file-1")).isTrue();
        verify(metadata).delete("file-1");
    }

    @Test
    void trustedInternalReadCompatibilityAndReadFailureArePreserved() throws Exception {
        when(storage.download("file-1")).thenReturn(content());
        assertThat(manager.getMetadata("file-1")).isSameAs(file);
        assertThat(manager.getFileMetadata("file-1")).isSameAs(file);
        assertThat(manager.getFileContentBase64("file-1")).isEqualTo("ZGF0YQ==");
        assertThat(manager.getFileContentBase64("missing")).isNull();
        when(storage.download("file-1")).thenThrow(new IllegalStateException("controlled read failure"));
        assertThat(manager.getFileContentBase64("file-1")).isNull();
    }

    @Test
    void storageRegistrationRefreshAndManagementDelegationRemainCompatible() {
        when(configs.getAllEnabledConfigs()).thenReturn(List.of(config));
        manager.refreshConfiguredStorages();
        verify(storage).init(config);
        assertThat(manager.getStorage("local")).isSameAs(storage);
        assertThat(manager.getStorage("missing")).isNull();
        when(storage.testConnection()).thenReturn(true);
        when(storage.createBucket("configured")).thenReturn(true);
        when(storage.bucketExists("configured")).thenReturn(true);
        assertThat(manager.testConnection("local")).isTrue();
        assertThat(manager.createBucket("local", "configured")).isTrue();
        assertThat(manager.bucketExists("local", "configured")).isTrue();
        assertThat(manager.deleteBucket("local", "configured")).isFalse();
        assertThatThrownBy(() -> manager.testConnection("missing")).isInstanceOf(RuntimeException.class);
        verify(storage).deleteBucket("configured");
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    private ByteArrayInputStream content() {
        return new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    }
}
