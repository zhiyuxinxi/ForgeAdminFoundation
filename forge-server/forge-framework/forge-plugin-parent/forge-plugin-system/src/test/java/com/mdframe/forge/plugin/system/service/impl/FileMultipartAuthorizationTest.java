package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual init/part/complete streams use isolated JUnit files, never customer or cloud storage. */
class FileMultipartAuthorizationTest {
    @TempDir Path directory;
    private final LoginUser caller = new LoginUser();
    private final StorageConfig config = new StorageConfig();
    private final SysFileMetadataMapper mapper = mock(SysFileMetadataMapper.class);
    private final Map<String, SysFileMetadata> rows = new HashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private MockedStatic<SessionHelper> identity;
    private MockedStatic<StpUtil> permissions;
    private MockMvc http;

    @BeforeAll
    static void initializeMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "multipart-test"),
                SysFileMetadata.class);
    }

    @BeforeEach
    void setUp() {
        caller.setUserId(100L);
        caller.setTenantId(7L);
        caller.setUserType(2);
        identity = mockStatic(SessionHelper.class);
        identity.when(SessionHelper::getLoginUser).thenReturn(caller);
        permissions = mockStatic(StpUtil.class);
        when(mapper.insert(any(SysFileMetadata.class))).thenAnswer(call -> {
            SysFileMetadata row = call.getArgument(0);
            rows.put(row.getFileId(), row);
            return 1;
        });
        when(mapper.selectOne(any())).thenAnswer(call -> {
            AbstractWrapper<?, ?, ?> wrapper = call.getArgument(0);
            wrapper.getSqlSegment();
            return rows.values().stream().filter(row -> wrapper.getParamNameValuePairs()
                    .containsValue(row.getFileId())).findFirst().orElse(null);
        });
        SystemFileMetadataPersistence persistence = new SystemFileMetadataPersistence(mapper);
        config.setStorageType("local");
        config.setBasePath(directory.toString());
        config.setAllowedTypes("txt");
        config.setMaxFileSize(1);
        LocalFileStorage storage = new LocalFileStorage();
        ReflectionTestUtils.setField(storage, "metadataPersistence", persistence);
        storage.init(config);
        StorageConfigProvider provider = mock(StorageConfigProvider.class);
        when(provider.getConfigByType("local")).thenReturn(config);
        when(provider.getDefaultConfig()).thenReturn(config);
        FileManager manager = new FileManager();
        manager.registerStorage(storage);
        ReflectionTestUtils.setField(manager, "configProvider", provider);
        ReflectionTestUtils.setField(manager, "metadataPersistence", persistence);
        http = MockMvcBuilders.standaloneSetup(new FileController(manager))
                .setControllerAdvice(new ErrorResponse()).build();
    }

    @AfterEach
    void closeIdentity() {
        permissions.close();
        identity.close();
    }

    @Test
    void ordinaryMultipartCreatesPrivateOwnedFileAndAnotherMemberCannotReadIt() throws Exception {
        String uploadId = initialize();
        String first = part(uploadId, 1, new byte[]{1, 2}, 200);
        String second = part(uploadId, 2, new byte[]{3, 4}, 200);
        SysFileMetadata result = complete(uploadId, List.of(first, second));
        assertThat(result.getUploaderId()).isEqualTo(100L);
        assertThat(result.getTenantId()).isEqualTo(7L);
        assertThat(result.getIsPrivate()).isTrue();
        assertThat(result.getBusinessType()).isEqualTo("documents");
        assertThat(result.getBusinessId()).isEqualTo("42");
        assertThat(Files.readAllBytes(directory.resolve(result.getFilePath()))).containsExactly(1, 2, 3, 4);
        caller.setUserId(200L);
        http.perform(get("/api/file/url/" + result.getFileId())).andExpect(status().isForbidden());
        caller.setUserId(100L);
        http.perform(get("/api/file/url/" + result.getFileId())).andExpect(status().isOk());
        http.perform(post("/api/file/multipart/complete").param("uploadId", uploadId)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(first, second))))
                .andExpect(status().isForbidden());
        assertThat(rows).hasSize(1);
    }

    @Test
    void ordinaryAndMultipartSharePublicVisibilityAuthorization() throws Exception {
        http.perform(post("/api/file/multipart/init").param("fileName", "report.txt").param("isPrivate", "false"))
                .andExpect(status().isForbidden());
        http.perform(multipart("/api/file/upload").file(new MockMultipartFile("file", "report.txt", "text/plain", new byte[]{1}))
                .param("isPrivate", "false")).andExpect(status().isForbidden());
        assertThat(rows).isEmpty();
        permissions.when(() -> StpUtil.hasPermission("*:*:*")).thenReturn(true);
        String response = http.perform(post("/api/file/multipart/init").param("fileName", "report.txt")
                        .param("isPrivate", "false")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String uploadId = json.readTree(response).path("data").asText();
        String etag = part(uploadId, 1, new byte[]{7}, 200);
        SysFileMetadata result = complete(uploadId, List.of(etag));
        assertThat(result.getIsPrivate()).isFalse();
        assertThat(result.getUploaderId()).isEqualTo(100L);
    }

    @Test
    void anotherUserTenantOrStorageCannotAppendOrCompleteCapturedUploadId() throws Exception {
        String uploadId = initialize();
        caller.setUserId(200L);
        part(uploadId, 1, new byte[]{9}, 403);
        assertCompleteStatus(uploadId, List.of("part_1"), 403);
        caller.setUserId(100L);
        caller.setTenantId(99L);
        part(uploadId, 1, new byte[]{9}, 403);
        assertCompleteStatus(uploadId, List.of("part_1"), 403);
        caller.setTenantId(7L);
        http.perform(multipart("/api/file/multipart/upload").file(chunk(new byte[]{9}))
                .param("uploadId", uploadId).param("partNumber", "1").param("storageType", "rustfs"))
                .andExpect(status().isForbidden());
        assertCompleteStatus("forged|bucket|other/key", List.of("tag"), 403);
        String etag = part(uploadId, 1, new byte[]{1}, 200);
        assertThat(complete(uploadId, List.of(etag)).getUploaderId()).isEqualTo(100L);
    }

    @Test
    void retransmissionUsesNetActualBytesAndSnapshotSurvivesConfigurationChange() throws Exception {
        String uploadId = initialize();
        part(uploadId, 1, new byte[600 * 1024], 200);
        String first = part(uploadId, 1, new byte[400 * 1024], 200);
        config.setAllowedTypes("pdf");
        config.setMaxFileSize(2);
        String second = part(uploadId, 2, new byte[600 * 1024], 200);
        SysFileMetadata result = complete(uploadId, List.of(first, second));
        assertThat(result.getFileSize()).isEqualTo(1000L * 1024);
        assertThat(Files.size(directory.resolve(result.getFilePath()))).isEqualTo(1000L * 1024);
    }

    @Test
    void cumulativeOverflowAbortsOnlyThatUploadAndCannotCreateVisibleMetadata() throws Exception {
        String kept = initialize();
        String keptTag = part(kept, 1, new byte[]{6}, 200);
        String rejected = initialize();
        part(rejected, 1, new byte[600 * 1024], 200);
        config.setMaxFileSize(2);
        part(rejected, 2, new byte[600 * 1024], 413);
        assertThat(rows).isEmpty();
        assertCompleteStatus(rejected, List.of("part_1", "part_2"), 403);
        try (var files = Files.walk(directory)) {
            assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(1);
        }
        assertThat(complete(kept, List.of(keptTag)).getFileSize()).isEqualTo(1L);
    }

    @Test
    void missingOrMismatchedPartsFailBeforeMergeAndOwnerCanRepairTheSequence() throws Exception {
        String uploadId = initialize();
        String second = part(uploadId, 2, new byte[]{2}, 200);
        assertCompleteStatus(uploadId, List.of(second), 400);
        String first = part(uploadId, 1, new byte[]{1}, 200);
        assertCompleteStatus(uploadId, List.of("forged", second), 400);
        assertThat(rows).isEmpty();
        SysFileMetadata result = complete(uploadId, List.of(first, second));
        assertThat(Files.readAllBytes(directory.resolve(result.getFilePath()))).containsExactly(1, 2);
    }

    @Test
    void invalidPartNumberAndUnauthenticatedInitializationHaveNoPublishedEffects() throws Exception {
        String uploadId = initialize();
        part(uploadId, 0, new byte[]{1}, 400);
        assertThat(rows).isEmpty();
        caller.setTenantId(null);
        http.perform(post("/api/file/multipart/init").param("fileName", "report.txt"))
                .andExpect(status().isForbidden());
    }

    private String initialize() throws Exception {
        String response = http.perform(post("/api/file/multipart/init").param("fileName", "report.txt")
                        .param("businessType", "documents").param("businessId", "42"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").asText();
    }

    private String part(String uploadId, int number, byte[] bytes, int expectedStatus) throws Exception {
        String response = http.perform(multipart("/api/file/multipart/upload").file(chunk(bytes))
                        .param("uploadId", uploadId).param("partNumber", String.valueOf(number)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").asText();
    }

    private MockMultipartFile chunk(byte[] bytes) {
        return new MockMultipartFile("file", "chunk.bin", "application/octet-stream", bytes);
    }

    private SysFileMetadata complete(String uploadId, List<String> etags) throws Exception {
        String response = assertCompleteStatus(uploadId, etags, 200);
        return rows.get(json.readTree(response).path("data").path("fileId").asText());
    }

    private String assertCompleteStatus(String uploadId, List<String> etags, int expectedStatus) throws Exception {
        return http.perform(post("/api/file/multipart/complete").param("uploadId", uploadId)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(etags)))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    @RestControllerAdvice
    static class ErrorResponse {
        @ExceptionHandler(BusinessException.class)
        ResponseEntity<Map<String, String>> handle(BusinessException error) {
            return ResponseEntity.status(error.getCode()).body(Map.of("message", error.getMessage()));
        }
    }
}
