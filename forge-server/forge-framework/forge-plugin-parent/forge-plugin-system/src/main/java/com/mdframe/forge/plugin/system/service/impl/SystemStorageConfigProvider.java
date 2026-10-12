package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.plugin.system.mapper.SysFileStorageConfigMapper;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 存储配置提供者实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemStorageConfigProvider implements StorageConfigProvider {
    
    private final SysFileStorageConfigMapper configMapper;
    private final SysFileMetadataMapper metadataMapper;
    
    @Override
    //@Cacheable(value = "storage_config", key = "'default'")
    public StorageConfig getDefaultConfig() {
        SysFileStorageConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<SysFileStorageConfig>()
                .eq(SysFileStorageConfig::getIsDefault, true)
                .eq(SysFileStorageConfig::getEnabled, true)
                .last("LIMIT 1")
        );
        
        if (config == null) {
            log.warn("未找到默认存储配置");
            return null;
        }
        
        return convertToStorageConfig(config);
    }
    
    @Override
    //@Cacheable(value = "storage_config", key = "#storageType")
    public StorageConfig getConfigByType(String storageType) {
        SysFileStorageConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<SysFileStorageConfig>()
                .eq(SysFileStorageConfig::getStorageType, storageType)
                .eq(SysFileStorageConfig::getEnabled, true)
                .last("LIMIT 1")
        );
        
        if (config == null) {
            log.warn("未找到存储配置: {}", storageType);
            return null;
        }
        
        return convertToStorageConfig(config);
    }
    
    @Override
    //@Cacheable(value = "storage_config", key = "'all'")
    public List<StorageConfig> getAllEnabledConfigs() {
        List<SysFileStorageConfig> configs = configMapper.selectList(
            new LambdaQueryWrapper<SysFileStorageConfig>()
                .eq(SysFileStorageConfig::getEnabled, true)
                .orderByAsc(SysFileStorageConfig::getOrderNum)
        );
        
        return configs.stream()
                .map(this::convertToStorageConfig)
                .collect(Collectors.toList());
    }
    
    @Override
    //@CacheEvict(value = "storage_config", allEntries = true)
    public void refreshConfig() {
        log.info("刷新存储配置缓存");
    }

    @Override
    public boolean hasActiveBucketReferences(String storageType, String bucketName) {
        LambdaQueryWrapper<SysFileMetadata> wrapper = new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getStorageType, storageType)
                .eq(SysFileMetadata::getStatus, 1);
        if ("local".equals(storageType)) {
            wrapper.and(reference -> reference.eq(SysFileMetadata::getBucket, bucketName).or()
                    .apply("LEFT(REPLACE(file_path, CHAR(92), '/'), CHAR_LENGTH({0}) + 1) = CONCAT({0}, '/')",
                            bucketName));
        } else {
            wrapper.eq(SysFileMetadata::getBucket, bucketName);
        }
        // A shared directory/bucket can contain records from every tenant.
        Long count = TenantContextHolder.executeIgnore(() -> metadataMapper.selectCount(wrapper));
        return count == null || count > 0;
    }
    
    /**
     * 转换为StorageConfig
     */
    private StorageConfig convertToStorageConfig(SysFileStorageConfig entity) {
        StorageConfig config = new StorageConfig();
        BeanUtil.copyProperties(entity, config);
        config.setId(entity.getId());
        return config;
    }
}
