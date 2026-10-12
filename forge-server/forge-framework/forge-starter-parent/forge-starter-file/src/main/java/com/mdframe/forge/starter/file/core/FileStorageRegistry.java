package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns registered storage strategies and refreshes their configured state. */
@Slf4j
final class FileStorageRegistry {
    private final Map<String, FileStorage> storages = new ConcurrentHashMap<>();

    void register(FileStorage storage) {
        storages.put(storage.getStorageType(), storage);
        log.info("注册文件存储策略: {}", storage.getStorageType());
    }

    FileStorage get(String storageType) {
        return storages.get(storageType);
    }

    FileStorage require(String storageType) {
        FileStorage storage = get(storageType);
        if (storage == null) {
            throw new RuntimeException(UiMessages.message("customer.storageType", storageType));
        }
        return storage;
    }

    void refresh(StorageConfigProvider configProvider) {
        if (configProvider == null) {
            log.warn("未配置StorageConfigProvider，跳过存储策略刷新");
            return;
        }

        configProvider.getAllEnabledConfigs().forEach(config -> {
            FileStorage storage = get(config.getStorageType());
            if (storage != null) {
                try {
                    storage.init(config);
                    log.info("刷新文件存储策略: {} - {}", config.getStorageType(), config.getConfigName());
                } catch (Exception e) {
                    log.warn("刷新文件存储策略失败: {} - {}", config.getStorageType(), config.getConfigName(), e);
                }
            }
        });
    }

}
