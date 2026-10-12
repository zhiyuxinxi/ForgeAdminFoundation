package com.mdframe.forge.starter.file.controller;

import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/** Platform storage management, retaining the original bucket URLs. */
@RestController
@RequestMapping("/api/file/bucket")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "forge.file.enable-generic-api", havingValue = "true", matchIfMissing = true)
@ApiPermissionIgnore
public class FileBucketController {
    private final FileManager fileManager;
    private final ObjectProvider<StorageConfigProvider> configProvider;

    @PostMapping
    public RespInfo<Boolean> create(@RequestParam String storageType, @RequestParam String bucketName) {
        return mutationResult(authorizedStorage(storageType, bucketName).createBucket(bucketName));
    }

    @GetMapping("/exists")
    public RespInfo<Boolean> exists(@RequestParam String storageType, @RequestParam String bucketName) {
        return RespInfo.success(authorizedStorage(storageType, bucketName).bucketExists(bucketName));
    }

    @DeleteMapping
    public RespInfo<Boolean> delete(@RequestParam String storageType, @RequestParam String bucketName) {
        FileStorage storage = authorizedStorage(storageType, bucketName);
        if (configProvider.getObject().hasActiveBucketReferences(storageType, bucketName)) {
            throw denied();
        }
        return mutationResult(storage.deleteBucket(bucketName));
    }

    private RespInfo<Boolean> mutationResult(boolean succeeded) {
        if (!succeeded) {
            throw new BusinessException(500, UiMessages.message("operation.failed"));
        }
        return RespInfo.success(true);
    }

    private FileStorage authorizedStorage(String storageType, String bucketName) {
        SessionHelper.assertAdmin(UiMessages.message("error.forbidden"));
        StorageConfigProvider provider = configProvider.getIfAvailable();
        StorageConfig config = provider == null ? null : provider.getConfigByType(storageType);
        if (config == null || config.getId() == null || !Boolean.TRUE.equals(config.getEnabled())
                || !Objects.equals(config.getStorageType(), storageType)
                || bucketName == null || bucketName.isBlank()
                || !Objects.equals(config.getBucketName(), bucketName)) {
            throw denied();
        }
        FileStorage storage = fileManager.getStorage(storageType);
        if (storage == null) {
            throw denied();
        }
        return storage;
    }

    private BusinessException denied() {
        return new BusinessException(403, UiMessages.message("error.forbidden"));
    }
}
