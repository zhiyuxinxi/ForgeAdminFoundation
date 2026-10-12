package com.mdframe.forge.starter.file.storage.impl;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.qcloud.cos.COSClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** SDK clients are mocked: these checks never contact or mutate a real bucket. */
class MultipartAbortAdapterTest {
    @Test
    void localCancellationFailureRetainsNativeContextForRetry(@TempDir Path directory) throws Exception {
        UiMessages.message("operation.failed");
        StorageConfig config = new StorageConfig();
        config.setBasePath(directory.toString());
        LocalFileStorage storage = new LocalFileStorage();
        storage.init(config);
        String uploadId = storage.initMultipartUpload("report.txt", "common", "42");
        String other = storage.initMultipartUpload("other.txt", "common", "43");
        storage.uploadPart(uploadId, 1, new ByteArrayInputStream(new byte[]{1}));
        storage.uploadPart(other, 1, new ByteArrayInputStream(new byte[]{2}));
        Path part = directory.resolve("multipart").resolve(uploadId).resolve("part_1");
        try (MockedStatic<Files> files = mockStatic(Files.class, call -> {
            if (call.getMethod().getName().equals("deleteIfExists") && part.equals(call.getArgument(0))) {
                throw new IOException("controlled deletion failure");
            }
            return call.callRealMethod();
        })) {
            assertThatThrownBy(() -> storage.abortMultipartUpload(uploadId)).hasRootCauseMessage("controlled deletion failure");
        }
        assertThat(part).exists();
        storage.abortMultipartUpload(uploadId);
        assertThat(part.getParent()).doesNotExist();
        assertThat(directory.resolve("multipart").resolve(other).resolve("part_1")).exists();
    }

    @Test
    void rustfsAbortsOnlyTheBoundNativeUpload() {
        S3Client client = mock(S3Client.class);
        RustfsFileStorage storage = new RustfsFileStorage();
        ReflectionTestUtils.setField(storage, "s3Client", client);
        storage.abortMultipartUpload("native-1|approved|owned/key.txt");
        ArgumentCaptor<AbortMultipartUploadRequest> request = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(client).abortMultipartUpload(request.capture());
        assertThat(request.getValue().uploadId()).isEqualTo("native-1");
        assertThat(request.getValue().bucket()).isEqualTo("approved");
        assertThat(request.getValue().key()).isEqualTo("owned/key.txt");
        verifyNoMoreInteractions(client);
    }

    @Test
    void tencentAbortsOnlyTheBoundNativeUpload() {
        COSClient client = mock(COSClient.class);
        TencentCosFileStorage storage = new TencentCosFileStorage();
        ReflectionTestUtils.setField(storage, "cosClient", client);
        storage.abortMultipartUpload("native-2|approved|owned/key.txt|key.txt|common|42");
        ArgumentCaptor<com.qcloud.cos.model.AbortMultipartUploadRequest> request =
                ArgumentCaptor.forClass(com.qcloud.cos.model.AbortMultipartUploadRequest.class);
        verify(client).abortMultipartUpload(request.capture());
        assertThat(request.getValue().getUploadId()).isEqualTo("native-2");
        assertThat(request.getValue().getBucketName()).isEqualTo("approved");
        assertThat(request.getValue().getKey()).isEqualTo("owned/key.txt");
        verifyNoMoreInteractions(client);
    }
}
