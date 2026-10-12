package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.config.FileStorageProperties;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import java.io.InputStream;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Authorized multipart lifecycle; opaque IDs never expose an editable storage/bucket/object descriptor. */
final class FileMultipartOperations {
    private final FileStorageRegistry storages;
    private final MultipartSessionRegistry sessions;

    FileMultipartOperations(FileStorageRegistry storages, Supplier<FileStorageProperties> properties,
                            LongSupplier clock) {
        this.storages = storages;
        this.sessions = new MultipartSessionRegistry(properties, clock);
    }

    String initialize(String fileName, String businessType, String businessId, String storageType,
                      Boolean isPrivate, StorageConfigProvider configs) {
        LoginUser user = currentUser();
        boolean privateFile = FileUploadPolicy.resolveVisibility(isPrivate);
        FileUploadPolicy.MultipartPolicy policy = new FileUploadPolicy(configs).multipartPolicy(fileName, storageType);
        FileStorage storage = storages.require(storageType);
        return sessions.create(expiresAt -> new MultipartUploadSession(storage,
                storage.initMultipartUpload(fileName, businessType, businessId), storageType, user,
                fileName, businessType, businessId, privateFile, policy, expiresAt));
    }

    String upload(String uploadId, int partNumber, InputStream input, String storageType) {
        MultipartUploadSession session = sessions.require(uploadId);
        synchronized (session) {
            session.authorize(currentUser(), storageType);
            sessions.requireUnexpired(uploadId, session);
            try {
                return session.upload(partNumber, input);
            } catch (RuntimeException failure) {
                sessions.abort(uploadId, session, failure);
                throw failure;
            }
        }
    }

    FileMetadata complete(String uploadId, List<String> etags, String storageType,
                          FileMetadataPersistence metadata) {
        MultipartUploadSession session = sessions.require(uploadId);
        synchronized (session) {
            session.authorize(currentUser(), storageType);
            sessions.requireUnexpired(uploadId, session);
            session.validateComplete(etags);
            try {
                FileMetadata result = session.complete(etags);
                if (metadata != null) {
                    metadata.save(result);
                }
                sessions.completed(uploadId, session);
                return result;
            } catch (RuntimeException failure) {
                sessions.abort(uploadId, session, failure);
                throw failure;
            }
        }
    }

    void reapExpired() {
        sessions.reapExpired();
    }

    void close() {
        sessions.close();
    }

    private LoginUser currentUser() {
        LoginUser user = SessionHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getTenantId() == null) {
            throw new BusinessException(403, UiMessages.message("error.forbidden"));
        }
        return user;
    }

}
