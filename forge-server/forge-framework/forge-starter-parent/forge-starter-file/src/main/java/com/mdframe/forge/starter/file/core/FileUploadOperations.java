package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.file.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/** Upload orchestration and metadata persistence, without storage implementation details. */
@Slf4j
final class FileUploadOperations {
    private final FileStorageRegistry storages;
    private final StorageConfigProvider configProvider;
    private final FileMetadataPersistence metadataPersistence;
    private final FileUploadPolicy policy;

    FileUploadOperations(FileStorageRegistry storages, StorageConfigProvider configProvider,
                         FileMetadataPersistence metadataPersistence) {
        this.storages = storages;
        this.configProvider = configProvider;
        this.metadataPersistence = metadataPersistence;
        this.policy = new FileUploadPolicy(configProvider);
    }

    String resolveStorageType(String storageType) {
        if (storageType != null) {
            return storageType;
        }
        if (configProvider == null) {
            throw new RuntimeException("未配置StorageConfigProvider");
        }
        StorageConfig config = configProvider.getDefaultConfig();
        if (config == null) {
            throw new RuntimeException("未找到默认存储配置");
        }
        return config.getStorageType();
    }

    FileMetadata uploadStream(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId,
                               String storageType, Boolean isPrivate, Long fileSize) {
        LoginUser user = FileUploadPolicy.requireUploadUser();
        boolean privateFile = FileUploadPolicy.resolveVisibility(isPrivate);
        if (inputStream == null) {
            throw new RuntimeException("文件流不能为空");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }
        storageType = resolveStorageType(storageType);
        policy.validateFileName(fileName, storageType, fileSize);

        FileStorage storage = storages.get(storageType);
        if (storage == null) {
            throw new RuntimeException(UiMessages.message("customer.storageType", storageType));
        }

        FileMetadata metadata = storage.upload(inputStream, fileName, contentType, businessType, businessId, fileSize);
        if (metadata == null) {
            throw new BusinessException(500, UiMessages.message("customer.uploadMetadataMissing"));
        }
        if (metadata.getFileSize() == null && fileSize != null) {
            metadata.setFileSize(fileSize);
        }
        metadata.setIsPrivate(privateFile);
        metadata.setUploaderId(user.getUserId());
        if (metadataPersistence != null) {
            metadataPersistence.save(metadata);
        }
        return metadata;
    }

    FileMetadata uploadMultipart(MultipartFile file, String businessType, String businessId,
                                  String storageType, Boolean isPrivate) {
        LoginUser user = FileUploadPolicy.requireUploadUser();
        boolean privateFile = FileUploadPolicy.resolveVisibility(isPrivate);
        // 验证文件
        policy.validateFile(file, storageType);

        // 秒传检查：仅当 businessType 和 businessId 也一致时才复用
        String md5 = FileUtil.calculateMd5(file);
        if (metadataPersistence != null) {
            FileMetadata existing = metadataPersistence.getByMd5(md5);
            if (existing != null
                    && java.util.Objects.equals(existing.getBusinessType(), businessType)
                    && java.util.Objects.equals(existing.getBusinessId(), businessId)
                    && java.util.Objects.equals(existing.getIsPrivate(), privateFile)
                    && (!Boolean.TRUE.equals(existing.getIsPrivate())
                        || java.util.Objects.equals(existing.getUploaderId(), user.getUserId()))) {
                log.info("文件秒传: md5={}, businessType={}", md5, businessType);
                return existing;
            }
        }

        // 获取存储策略并上传
        FileStorage storage = storages.get(storageType);
        if (storage == null) {
            throw new RuntimeException(UiMessages.message("customer.storageType", storageType));
        }

        FileMetadata metadata = storage.upload(file, businessType, businessId);
        if (metadata == null) {
            throw new BusinessException(500, UiMessages.message("customer.uploadMetadataMissing"));
        }
        metadata.setMd5(md5);
        metadata.setIsPrivate(privateFile);
        metadata.setUploaderId(user.getUserId());

        // 持久化元数据
        if (metadataPersistence != null) {
            metadataPersistence.save(metadata);
        }

        return metadata;
    }
}
