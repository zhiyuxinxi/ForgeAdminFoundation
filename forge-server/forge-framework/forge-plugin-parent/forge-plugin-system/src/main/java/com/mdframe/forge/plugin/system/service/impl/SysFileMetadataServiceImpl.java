package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.entity.SysFileGroup;
import com.mdframe.forge.plugin.system.dto.SysFileMetadataUpdateDTO;
import com.mdframe.forge.plugin.system.mapper.SysFileGroupMapper;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.core.FileManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.io.Serializable;

/**
 * 文件元数据Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileMetadataServiceImpl extends ServiceImpl<SysFileMetadataMapper, SysFileMetadata>
        implements ISysFileMetadataService {
    
    private final FileManager fileManager;
    private final SysFileGroupMapper groupMapper;
    
    @Override
    public Page<SysFileMetadata> page(PageQuery query, SysFileMetadata condition) {
        LambdaQueryWrapper<SysFileMetadata> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(condition.getOriginalName())) {
            wrapper.like(SysFileMetadata::getOriginalName, condition.getOriginalName());
        }
        
        if (StrUtil.isNotBlank(condition.getStorageType())) {
            wrapper.eq(SysFileMetadata::getStorageType, condition.getStorageType());
        }
        
        if (StrUtil.isNotBlank(condition.getBusinessType())) {
            wrapper.eq(SysFileMetadata::getBusinessType, condition.getBusinessType());
        }
        
        if (StrUtil.isNotBlank(condition.getBusinessId())) {
            wrapper.eq(SysFileMetadata::getBusinessId, condition.getBusinessId());
        }
        
        if (condition.getUploaderId() != null) {
            wrapper.eq(SysFileMetadata::getUploaderId, condition.getUploaderId());
        }
        
        if (condition.getGroupId() != null) {
            wrapper.eq(SysFileMetadata::getGroupId, condition.getGroupId());
        }
        
        if (StrUtil.isNotBlank(condition.getMimeType())) {
            wrapper.likeRight(SysFileMetadata::getMimeType, condition.getMimeType());
        }

        if (condition.getIsPrivate() != null) {
            wrapper.eq(SysFileMetadata::getIsPrivate, condition.getIsPrivate());
        }

        FileMetadataAccess.applyReadScope(wrapper);
        
        wrapper.orderByDesc(SysFileMetadata::getUploadTime);
        
        Page<SysFileMetadata> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.baseMapper.selectPage(page, wrapper);
    }
    
    @Override
    public List<SysFileMetadata> listByBusiness(String businessType, String businessId) {
        LambdaQueryWrapper<SysFileMetadata> wrapper = new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getBusinessType, businessType)
                .eq(SysFileMetadata::getBusinessId, businessId)
                .orderByDesc(SysFileMetadata::getUploadTime);
        FileMetadataAccess.applyReadScope(wrapper);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public SysFileMetadata getById(Serializable id) {
        SysFileMetadata metadata = baseMapper.selectById(id);
        if (metadata != null) {
            FileMetadataAccess.requireRead(metadata);
        }
        return metadata;
    }
    
    @Override
    public SysFileMetadata getByFileId(String fileId) {
        SysFileMetadata metadata = findActive(fileId);
        if (metadata != null) {
            FileMetadataAccess.requireRead(metadata);
        }
        return metadata;
    }

    @Override
    public void removeByFileId(String fileId) {
        SysFileMetadata metadata = findActive(fileId);
        FileMetadataAccess.requireWrite(metadata);
        // 文件 IO 在事务外执行，避免长事务占用 DB 连接
        deleteAuthorized(metadata);
    }

    @Override
    public void removeBatch(String[] fileIds) {
        List<SysFileMetadata> authorized = new ArrayList<>();
        for (String id : fileIds) {
            SysFileMetadata metadata = baseMapper.selectById(id);
            FileMetadataAccess.requireWrite(metadata);
            authorized.add(metadata);
        }
        for (SysFileMetadata metadata : authorized) {
            deleteAuthorized(metadata);
        }
    }

    @Override
    public void rename(String fileId, String originalName) {
        SysFileMetadata existing = findActive(fileId);
        FileMetadataAccess.requireWrite(existing);
        baseMapper.update(null, new LambdaUpdateWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getFileId, fileId)
                .eq(SysFileMetadata::getTenantId, existing.getTenantId())
                .set(SysFileMetadata::getOriginalName, originalName));
    }

    @Override
    public boolean updateGroup(SysFileMetadataUpdateDTO update) {
        SysFileMetadata existing = baseMapper.selectById(update.getId());
        FileMetadataAccess.requireWrite(existing);
        if (update.getGroupId() != null) {
            SysFileGroup group = groupMapper.selectById(update.getGroupId());
            if (group == null || !Objects.equals(group.getTenantId(), existing.getTenantId())
                    || !Objects.equals(group.getStatus(), 1)) {
                throw new BusinessException(403, UiMessages.message("customer.fileOwnership"));
            }
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getId, existing.getId())
                .eq(SysFileMetadata::getTenantId, existing.getTenantId())
                .set(SysFileMetadata::getGroupId, update.getGroupId())) > 0;
    }

    private SysFileMetadata findActive(String fileId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysFileMetadata>()
                .eq(SysFileMetadata::getFileId, fileId).eq(SysFileMetadata::getStatus, 1));
    }

    private void deleteAuthorized(SysFileMetadata metadata) {
        if (!fileManager.delete(metadata.getFileId())) {
            throw new BusinessException(UiMessages.translate("文件删除失败"));
        }
    }
}
