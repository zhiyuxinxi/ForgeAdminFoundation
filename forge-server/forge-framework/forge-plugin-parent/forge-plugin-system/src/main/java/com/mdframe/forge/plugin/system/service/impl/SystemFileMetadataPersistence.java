package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文件元数据持久化实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemFileMetadataPersistence implements FileMetadataPersistence {
    
    private final SysFileMetadataMapper metadataMapper;
    
    @Override
    public void save(FileMetadata metadata) {
        LoginUser user = FileMetadataAccess.currentUser();
        metadata.setUploaderId(user.getUserId());
        SysFileMetadata entity = new SysFileMetadata();
        BeanUtil.copyProperties(metadata, entity);
        entity.setTenantId(user.getTenantId());
        if (entity.getIsPrivate() == null) {
            entity.setIsPrivate(true);
            metadata.setIsPrivate(true);
        }
        entity.setStatus(1);
        metadataMapper.insert(entity);
    }
    
    @Override
    public FileMetadata getById(String fileId) {
        SysFileMetadata entity = metadataMapper.selectOne(
            new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getFileId, fileId)
                .eq(SysFileMetadata::getStatus, 1)
        );
        
        if (entity == null) {
            return null;
        }
        
        return convertToFileMetadata(entity);
    }
    
    @Override
    public FileMetadata getByMd5(String md5) {
        if (md5 == null || md5.isEmpty()) {
            return null;
        }
        
        LambdaQueryWrapper<SysFileMetadata> wrapper = new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getMd5, md5).last("LIMIT 1");
        FileMetadataAccess.applyReadScope(wrapper);
        SysFileMetadata entity = metadataMapper.selectOne(wrapper);
        
        if (entity == null) {
            return null;
        }
        
        return convertToFileMetadata(entity);
    }
    
    @Override
    public void incrementDownloadCount(String fileId) {
        metadataMapper.incrementDownloadCount(fileId);
    }
    
    @Override
    public void delete(String fileId) {
        // 逻辑删除
        metadataMapper.update(null,
            new LambdaUpdateWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getFileId, fileId)
                .set(SysFileMetadata::getStatus, 0)
        );
    }
    
    @Override
    public boolean checkPermission(String fileId, Long userId) {
        SysFileMetadata entity = findActive(fileId);
        return matchesCurrentUser(userId) && FileMetadataAccess.canRead(entity);
    }

    @Override
    public boolean checkWritePermission(String fileId, Long userId) {
        SysFileMetadata entity = findActive(fileId);
        return matchesCurrentUser(userId) && FileMetadataAccess.canWrite(entity);
    }

    private boolean matchesCurrentUser(Long userId) {
        return userId == null || userId.equals(FileMetadataAccess.currentUser().getUserId());
    }

    private SysFileMetadata findActive(String fileId) {
        return metadataMapper.selectOne(
            new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getFileId, fileId)
                .eq(SysFileMetadata::getStatus, 1)
        );
    }
    
    /**
     * 转换为FileMetadata
     */
    private FileMetadata convertToFileMetadata(SysFileMetadata entity) {
        return FileMetadata.builder()
                .fileId(entity.getFileId())
                .originalName(entity.getOriginalName())
                .storageName(entity.getStorageName())
                .filePath(entity.getFilePath())
                .fileSize(entity.getFileSize())
                .mimeType(entity.getMimeType())
                .extension(entity.getExtension())
                .md5(entity.getMd5())
                .storageType(entity.getStorageType())
                .bucket(entity.getBucket())
                .accessUrl(entity.getAccessUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .businessType(entity.getBusinessType())
                .businessId(entity.getBusinessId())
                .uploaderId(entity.getUploaderId())
                .uploadTime(entity.getUploadTime())
                .expireTime(entity.getExpireTime())
                .isPrivate(entity.getIsPrivate())
                .downloadCount(entity.getDownloadCount())
                .build();
    }
}
