package com.mdframe.forge.starter.file.core;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

/** Configured upload size, extension and MIME rules shared by upload entry points. */
final class FileUploadPolicy {
    private static final long DEFAULT_MAX_FILE_SIZE_MB = 100L;

    static final String DEFAULT_ALLOWED_TYPES =
            "jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3";

    private final StorageConfigProvider configProvider;

    FileUploadPolicy(StorageConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    static boolean resolveVisibility(Boolean isPrivate) {
        if (Boolean.FALSE.equals(isPrivate) && !StpUtil.hasPermission("*:*:*")) {
            throw new BusinessException(403, UiMessages.message("customer.publicUploadDenied"));
        }
        return !Boolean.FALSE.equals(isPrivate);
    }

    static LoginUser requireUploadUser() {
        LoginUser user = SessionHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getTenantId() == null) {
            throw new BusinessException(403, UiMessages.message("error.forbidden"));
        }
        return user;
    }

    MultipartPolicy multipartPolicy(String fileName, String storageType) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(400, UiMessages.message("customer.invalidParameters"));
        }
        StorageConfig config = resolveValidationConfig(storageType);
        String extension = FileTypePolicy.requireSafeExtension(fileName);
        Set<String> allowedTypes = Set.copyOf(resolveAllowedTypes(config));
        FileTypePolicy.validateAllowedType(extension, allowedTypes, null);
        return new MultipartPolicy(maxFileSizeMb(config) * 1024L * 1024L, allowedTypes);
    }

    record MultipartPolicy(long maxBytes, Set<String> allowedTypes) {
        void validate(String fileName, long bytes) {
            if (bytes > maxBytes) {
                throw new BusinessException(413, UiMessages.message("customer.fileSize", maxBytes / 1024 / 1024));
            }
            FileTypePolicy.validateAllowedType(FileTypePolicy.requireSafeExtension(fileName), allowedTypes, null);
        }
    }

    void validateFile(MultipartFile file, String storageType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        validateFilePolicy(file.getOriginalFilename(), storageType, file.getSize(), file.getContentType());
    }

    void validateFileName(String fileName, String storageType, Long fileSize) {
        validateFilePolicy(fileName, storageType, fileSize, null);
    }

    private void validateFilePolicy(String fileName, String storageType, Long fileSize, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }

        StorageConfig config = resolveValidationConfig(storageType);
        long maxFileSizeMb = maxFileSizeMb(config);
        if (fileSize != null && fileSize >= 0) {
            long maxSize = maxFileSizeMb * 1024L * 1024L;
            if (fileSize > maxSize) {
                throw new RuntimeException(UiMessages.message("customer.fileSize", maxFileSizeMb));
            }
        }

        String extension = FileTypePolicy.requireSafeExtension(fileName);
        FileTypePolicy.validateAllowedType(extension, resolveAllowedTypes(config), contentType);
    }

    private long maxFileSizeMb(StorageConfig config) {
        return config != null && config.getMaxFileSize() != null && config.getMaxFileSize() > 0
                ? config.getMaxFileSize() : DEFAULT_MAX_FILE_SIZE_MB;
    }

    private StorageConfig resolveValidationConfig(String storageType) {
        if (configProvider == null) {
            return null;
        }
        if (storageType != null && !storageType.isBlank()) {
            return configProvider.getConfigByType(storageType);
        }
        return configProvider.getDefaultConfig();
    }

    private Set<String> resolveAllowedTypes(StorageConfig config) {
        if (config == null || config.getAllowedTypes() == null || config.getAllowedTypes().isBlank()) {
            throw new RuntimeException("文件存储配置未设置允许的文件类型");
        }
        Set<String> configuredTypes = config.getAllowedTypeList().stream()
                .map(FileTypePolicy::normalizeExtension)
                .filter(FileTypePolicy::isSafeExtension)
                .collect(java.util.stream.Collectors.toSet());
        if (configuredTypes.isEmpty()) {
            throw new RuntimeException("文件存储配置未设置有效的允许文件类型");
        }
        return configuredTypes;
    }

}
