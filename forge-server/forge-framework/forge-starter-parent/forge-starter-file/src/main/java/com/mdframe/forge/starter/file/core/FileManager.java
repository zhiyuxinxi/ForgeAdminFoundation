package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.file.config.FileStorageProperties;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

/** Compatible application entry point; lifecycle responsibilities live in focused collaborators. */
@Component
public class FileManager {
    public static final String DEFAULT_ALLOWED_TYPES = FileUploadPolicy.DEFAULT_ALLOWED_TYPES;

    private final FileStorageRegistry storages = new FileStorageRegistry();

    @Autowired(required = false)
    private FileStorageProperties storageProperties = new FileStorageProperties();

    private final FileMultipartOperations multipart =
            new FileMultipartOperations(storages, () -> storageProperties, System::currentTimeMillis);

    @Autowired(required = false)
    private StorageConfigProvider configProvider;

    @Autowired(required = false)
    private FileMetadataPersistence metadataPersistence;

    public void registerStorage(FileStorage storage) {
        storages.register(storage);
    }

    public FileStorage getStorage(String storageType) {
        return storages.get(storageType);
    }

    /** Internal metadata lookup; external file requests use the authorized application/service entry points. */
    public FileMetadata getMetadata(String fileId) {
        return content().getTrustedMetadata(fileId);
    }

    public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
        return upload(file, businessType, businessId, null, null);
    }

    public FileMetadata upload(MultipartFile file, String businessType, String businessId,
                               String storageType, Boolean isPrivate) {
        FileUploadOperations operations = uploads();
        return operations.uploadMultipart(file, businessType, businessId,
                operations.resolveStorageType(storageType), isPrivate);
    }

    public FileMetadata upload(MultipartFile file, String businessType, String businessId, String storageType) {
        return uploads().uploadMultipart(file, businessType, businessId, storageType, null);
    }

    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId) {
        return upload(inputStream, fileName, contentType, businessType, businessId, null, null);
    }

    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId, String storageType, Boolean isPrivate) {
        return upload(inputStream, fileName, contentType, businessType, businessId, storageType, isPrivate, null);
    }

    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId,
                               String storageType, Boolean isPrivate, Long fileSize) {
        return uploads().uploadStream(inputStream, fileName, contentType, businessType, businessId,
                storageType, isPrivate, fileSize);
    }

    public void download(String fileId, HttpServletResponse response) {
        content().download(fileId, response);
    }

    public String getAccessUrl(String fileId, Integer expires) {
        return content().getAccessUrl(fileId, expires);
    }

    /** Retains the trusted internal attachment-reading contract, not an external download authorization path. */
    public String getFileContentBase64(String fileId) {
        return content().getTrustedContentBase64(fileId);
    }

    /** Internal compatibility alias of getMetadata. */
    public FileMetadata getFileMetadata(String fileId) {
        return content().getTrustedMetadata(fileId);
    }

    public boolean delete(String fileId) {
        return content().delete(fileId);
    }

    public String initMultipartUpload(String fileName, String businessType, String businessId, String storageType) {
        return initMultipartUpload(fileName, businessType, businessId, storageType, true);
    }

    public String initMultipartUpload(String fileName, String businessType, String businessId,
                                      String storageType, Boolean isPrivate) {
        return multipart.initialize(fileName, businessType, businessId, storageType, isPrivate, configProvider);
    }

    public String uploadPart(String uploadId, int partNumber, InputStream inputStream, String storageType) {
        return multipart.upload(uploadId, partNumber, inputStream, storageType);
    }

    public FileMetadata completeMultipartUpload(String uploadId, List<String> partETags, String storageType) {
        return multipart.complete(uploadId, partETags, storageType, metadataPersistence);
    }

    public boolean testConnection(String storageType) {
        return storages.require(storageType).testConnection();
    }

    public boolean createBucket(String storageType, String bucketName) {
        return storages.require(storageType).createBucket(bucketName);
    }

    public boolean deleteBucket(String storageType, String bucketName) {
        return storages.require(storageType).deleteBucket(bucketName);
    }

    public boolean bucketExists(String storageType, String bucketName) {
        return storages.require(storageType).bucketExists(bucketName);
    }

    public void refreshConfiguredStorages() {
        storages.refresh(configProvider);
    }

    /** Uses the application's existing Spring scheduling; expired resources also release during idle periods. */
    @Scheduled(fixedDelayString = "${forge.file.multipart-cleanup-interval-ms:60000}")
    public void cleanupExpiredMultipartUploads() {
        multipart.reapExpired();
    }

    @PreDestroy
    public void closeMultipartUploads() {
        multipart.close();
    }

    // Capture the optional SPI bindings for this operation without adding new Spring bean requirements.
    private FileUploadOperations uploads() {
        return new FileUploadOperations(storages, configProvider, metadataPersistence);
    }

    private FileContentOperations content() {
        return new FileContentOperations(storages, metadataPersistence);
    }
}
