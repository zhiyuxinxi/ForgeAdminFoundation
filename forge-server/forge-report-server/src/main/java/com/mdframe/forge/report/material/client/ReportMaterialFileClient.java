package com.mdframe.forge.report.material.client;

import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 报表素材使用的通用文件客户端。
 */
@Component
@RequiredArgsConstructor
public class ReportMaterialFileClient {

    private final FileManager fileManager;
    private final ISysFileMetadataService fileMetadataService;

    public FileMetadata getMetadata(String fileId) {
        return fileManager.getMetadata(fileId);
    }

    public boolean delete(String fileId) {
        return fileManager.delete(fileId);
    }

    public void rename(String fileId, String originalName) {
        fileMetadataService.rename(fileId, originalName);
    }
}
