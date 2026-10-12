package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.config.FileStorageProperties;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MultipartSessionLifecycleTest {
    private final FileStorageProperties limits = new FileStorageProperties();
    private final AtomicLong clock = new AtomicLong();
    private final FileStorage storage = mock(FileStorage.class);
    private final StorageConfigProvider configs = mock(StorageConfigProvider.class);
    private final LoginUser user = new LoginUser();
    private FileMultipartOperations uploads;
    private MockedStatic<SessionHelper> identity;

    @BeforeEach
    void setUp() {
        user.setUserId(100L);
        user.setTenantId(7L);
        identity = mockStatic(SessionHelper.class);
        identity.when(SessionHelper::getLoginUser).thenReturn(user);
        limits.setMultipartMaxSessions(2);
        limits.setMultipartUploadTimeout(Duration.ofMillis(100));
        StorageConfig config = new StorageConfig();
        config.setAllowedTypes("txt");
        config.setMaxFileSize(1);
        when(configs.getConfigByType("local")).thenReturn(config);
        when(storage.getStorageType()).thenReturn("local");
        AtomicInteger nativeIds = new AtomicInteger();
        when(storage.initMultipartUpload(anyString(), anyString(), anyString()))
                .thenAnswer(call -> "native-" + nativeIds.incrementAndGet());
        FileStorageRegistry storages = new FileStorageRegistry();
        storages.register(storage);
        uploads = new FileMultipartOperations(storages, () -> limits, clock::get);
    }

    @AfterEach
    void closeIdentity() {
        identity.close();
    }

    @Test
    void capacityRejectsBeforeCreatingAnotherNativeUpload() {
        initialize();
        initialize();
        assertStatus(this::initialize, 429);
        verify(storage, times(2)).initMultipartUpload("report.txt", "common", "42");
        verify(storage, never()).abortMultipartUpload(anyString());
    }

    @Test
    void expiryReclaimsOnlyExpiredNativeIdsAndDoesNotExtendOnPartRetry() {
        String expired = initialize();
        clock.set(50);
        String active = initialize();
        when(storage.uploadPart(eq("native-1"), eq(1), any(InputStream.class))).thenAnswer(call -> {
            call.getArgument(2, InputStream.class).readAllBytes();
            return "etag";
        });
        clock.set(99);
        uploads.upload(expired, 1, new ByteArrayInputStream(new byte[]{1}), "local");
        clock.set(100);
        uploads.reapExpired();
        verify(storage).abortMultipartUpload("native-1");
        verify(storage, never()).abortMultipartUpload("native-2");
        assertStatus(() -> uploads.complete(expired, List.of("etag"), "local", null), 403);
        assertStatus(() -> uploads.complete(active, List.of("missing"), "local", null), 400);
        assertThat(initialize()).isNotBlank();
    }

    @Test
    void failedExpiryCancellationRetainsNativeIdAndCapacityUntilRetrySucceeds() {
        limits.setMultipartMaxSessions(1);
        String uploadId = initialize();
        doThrow(new IllegalStateException("controlled cancellation failure"))
                .when(storage).abortMultipartUpload("native-1");
        clock.set(100);
        uploads.reapExpired();
        assertStatus(this::initialize, 429);
        assertStatus(() -> uploads.complete(uploadId, List.of("tag"), "local", null), 403);
        verify(storage, times(1)).initMultipartUpload(anyString(), anyString(), anyString());
        doNothing().when(storage).abortMultipartUpload("native-1");
        uploads.reapExpired();
        assertThat(initialize()).isNotBlank();
        verify(storage, times(3)).abortMultipartUpload("native-1");
    }

    @Test
    void failedPartCancellationAlsoRetainsCapacityAndPreservesOriginalError() {
        limits.setMultipartMaxSessions(1);
        String uploadId = initialize();
        when(storage.uploadPart(anyString(), anyInt(), any(InputStream.class)))
                .thenThrow(new IllegalStateException("part failed"));
        doThrow(new IllegalStateException("abort failed")).when(storage).abortMultipartUpload("native-1");
        assertThatThrownBy(() -> uploads.upload(uploadId, 1, new ByteArrayInputStream(new byte[]{1}), "local"))
                .hasMessage("part failed").satisfies(error -> assertThat(error.getSuppressed()).hasSize(1));
        assertStatus(this::initialize, 429);
        doNothing().when(storage).abortMultipartUpload("native-1");
        uploads.reapExpired();
        assertThat(initialize()).isNotBlank();
    }

    @Test
    void successfulCompletionAndFailedInitializationReleaseTheirOwnCapacity() {
        limits.setMultipartMaxSessions(1);
        when(storage.initMultipartUpload(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("init failed")).thenReturn("native-success", "native-next");
        assertThatThrownBy(this::initialize).hasMessage("init failed");
        String uploadId = initialize();
        when(storage.uploadPart(eq("native-success"), eq(1), any(InputStream.class))).thenAnswer(call -> {
            call.getArgument(2, InputStream.class).readAllBytes();
            return "etag";
        });
        when(storage.completeMultipartUpload("native-success", List.of("etag")))
                .thenReturn(FileMetadata.builder().fileSize(1L).build());
        uploads.upload(uploadId, 1, new ByteArrayInputStream(new byte[]{1}), "local");
        uploads.complete(uploadId, List.of("etag"), "local", null);
        assertThat(initialize()).isNotBlank();
        verify(storage, never()).abortMultipartUpload("native-success");
    }

    @Test
    void inFlightNativeCreationAlreadyConsumesACapacitySlot() throws Exception {
        limits.setMultipartMaxSessions(1);
        MultipartSessionRegistry registry = new MultipartSessionRegistry(() -> limits, clock::get);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> registry.create(expiresAt -> {
                entered.countDown();
                try {
                    if (!release.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test latch timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return new MultipartUploadSession(storage, "owned", "local", user, "report.txt", "common", "42",
                        true, new FileUploadPolicy.MultipartPolicy(1024, java.util.Set.of("txt")), expiresAt);
            }));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertStatus(() -> registry.create(expiry -> { throw new AssertionError("must not create native upload"); }), 429);
            release.countDown();
            assertThat(first.get(2, TimeUnit.SECONDS)).isNotBlank();
            registry.close();
            verify(storage).abortMultipartUpload("owned");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void existingSpringSchedulingReclaimsExpiredUploadsWithoutAnotherRequest() {
        limits.setMultipartUploadTimeout(Duration.ofMillis(60));
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("multipart-test",
                    Map.of("forge.file.multipart-cleanup-interval-ms", "10")));
            context.registerBean(FileStorageProperties.class, () -> limits);
            context.registerBean(StorageConfigProvider.class, () -> configs);
            context.register(Scheduling.class, FileManager.class);
            context.refresh();
            FileManager manager = context.getBean(FileManager.class);
            manager.registerStorage(storage);
            manager.initMultipartUpload("report.txt", "common", "42", "local");
            verify(storage, timeout(1500)).abortMultipartUpload("native-1");
        }
    }

    private String initialize() {
        return uploads.initialize("report.txt", "common", "42", "local", true, configs);
    }

    private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, int status) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(status));
    }

    @Configuration
    @EnableScheduling
    static class Scheduling {
    }
}
