package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.storage.FileStorage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One trusted upload and its successful parts; callers synchronize its upload/complete transitions. */
final class MultipartUploadSession {
    final FileStorage storage;
    final String nativeId;
    final long expiresAt;
    private final String storageType;
    private final Long userId;
    private final Long tenantId;
    private final String fileName;
    private final String businessType;
    private final String businessId;
    private final boolean isPrivate;
    private final FileUploadPolicy.MultipartPolicy policy;
    private final Map<Integer, Part> parts = new HashMap<>();
    private long totalBytes;
    boolean finished;

    MultipartUploadSession(FileStorage storage, String nativeId, String storageType, LoginUser user,
                           String fileName, String businessType, String businessId, boolean isPrivate,
                           FileUploadPolicy.MultipartPolicy policy, long expiresAt) {
        this.storage = storage;
        this.nativeId = nativeId;
        this.storageType = storageType;
        this.userId = user.getUserId();
        this.tenantId = user.getTenantId();
        this.fileName = fileName;
        this.businessType = businessType;
        this.businessId = businessId;
        this.isPrivate = isPrivate;
        this.policy = policy;
        this.expiresAt = expiresAt;
    }

    void authorize(LoginUser user, String requestedStorage) {
        if (finished || !Objects.equals(userId, user.getUserId())
                || !Objects.equals(tenantId, user.getTenantId()) || !Objects.equals(storageType, requestedStorage)) {
            throw new BusinessException(403, UiMessages.message("error.forbidden"));
        }
    }

    String upload(int partNumber, InputStream input) {
        if (partNumber < 1 || partNumber > 10000 || input == null) {
            throw invalidParts();
        }
        Part previous = parts.get(partNumber);
        long retainedBytes = totalBytes - (previous == null ? 0 : previous.bytes());
        MultipartPartInputStream bounded = new MultipartPartInputStream(input,
                policy.maxBytes() - retainedBytes, policy.maxBytes());
        String etag = storage.uploadPart(nativeId, partNumber, bounded);
        if (etag == null || etag.isBlank() || bounded.count() == 0) {
            throw invalidParts();
        }
        totalBytes = retainedBytes + bounded.count();
        parts.put(partNumber, new Part(bounded.count(), etag));
        return etag;
    }

    void validateComplete(List<String> etags) {
        if (etags == null || etags.isEmpty() || etags.size() != parts.size()) {
            throw invalidParts();
        }
        for (int number = 1; number <= etags.size(); number++) {
            Part part = parts.get(number);
            if (part == null || !Objects.equals(part.etag(), etags.get(number - 1))) {
                throw invalidParts();
            }
        }
        policy.validate(fileName, totalBytes);
    }

    FileMetadata complete(List<String> etags) {
        FileMetadata result = storage.completeMultipartUpload(nativeId, etags);
        if (result == null || (result.getFileSize() != null && result.getFileSize() != totalBytes)) {
            throw invalidParts();
        }
        result.setFileSize(totalBytes);
        result.setOriginalName(fileName);
        result.setStorageType(storageType);
        result.setBusinessType(businessType);
        result.setBusinessId(businessId);
        result.setIsPrivate(isPrivate);
        result.setUploaderId(userId);
        return result;
    }

    private BusinessException invalidParts() {
        return new BusinessException(400, UiMessages.message("customer.invalidParameters"));
    }

    private record Part(long bytes, String etag) {
    }
}
