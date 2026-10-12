package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Existing read/write authorization stays ahead of metadata and storage side effects. */
@Slf4j
final class FileContentOperations {
    private final FileStorageRegistry storages;
    private final FileMetadataPersistence metadataPersistence;

    FileContentOperations(FileStorageRegistry storages, FileMetadataPersistence metadataPersistence) {
        this.storages = storages;
        this.metadataPersistence = metadataPersistence;
    }

    void download(String fileId, HttpServletResponse response) {
        requireFilePermission(fileId, false);
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }

        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            throw new RuntimeException(UiMessages.message("customer.fileMissing", fileId));
        }

        FileStorage storage = storages.get(metadata.getStorageType());
        if (storage == null) {
            throw new RuntimeException(UiMessages.message("customer.storageStrategy", metadata.getStorageType()));
        }

        try (InputStream inputStream = storage.download(fileId);
             OutputStream outputStream = response.getOutputStream()) {

            response.setContentType(metadata.getMimeType());
            response.setHeader("Content-Disposition",
                "attachment;filename=" + java.net.URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8));

            inputStream.transferTo(outputStream);

            // 更新下载次数
            metadataPersistence.incrementDownloadCount(fileId);

        } catch (Exception e) {
            log.error("文件下载失败: {}", fileId, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 获取文件访问URL
     */
    String getAccessUrl(String fileId, Integer expires) {
        requireFilePermission(fileId, false);
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }

        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            throw new RuntimeException(UiMessages.message("customer.fileMissing", fileId));
        }

        FileStorage storage = storages.get(metadata.getStorageType());
        if (storage == null) {
            throw new RuntimeException(UiMessages.message("customer.storageStrategy", metadata.getStorageType()));
        }

        return storage.getAccessUrl(fileId, expires);
    }

    /**
     * 获取文件内容的Base64编码
     */
    String getTrustedContentBase64(String fileId) {
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }

        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            return null;
        }

        FileStorage storage = storages.get(metadata.getStorageType());
        if (storage == null) {
            return null;
        }

        try (InputStream inputStream = storage.download(fileId)) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("获取文件Base64失败: {}", fileId, e);
            return null;
        }
    }

    /**
     * 获取文件元数据
     */
    FileMetadata getTrustedMetadata(String fileId) {
        if (metadataPersistence == null) {
            return null;
        }
        return metadataPersistence.getById(fileId);
    }

    /**
     * 删除文件
     */
    boolean delete(String fileId) {
        requireFilePermission(fileId, true);
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }

        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            return false;
        }

        FileStorage storage = storages.get(metadata.getStorageType());
        if (storage == null || !storage.delete(fileId)) {
            return false;
        }

        metadataPersistence.delete(fileId);
        return true;
    }

    private void requireFilePermission(String fileId, boolean write) {
        if (metadataPersistence == null) {
            throw new IllegalStateException("未配置FileMetadataPersistence");
        }
        boolean allowed = write ? metadataPersistence.checkWritePermission(fileId, null)
                : metadataPersistence.checkPermission(fileId, null);
        if (!allowed) {
            throw new BusinessException(403, UiMessages.message("customer.fileOwnership"));
        }
    }

}
