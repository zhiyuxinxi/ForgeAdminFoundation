package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.util.FileUtil;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Filename and declared media-type compatibility, independent of storage configuration and size. */
final class FileTypePolicy {
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "jsp", "jspx", "php", "asp", "aspx", "html", "htm",
            "js", "mjs", "ts", "vue", "sh", "bash", "bat", "cmd",
            "ps1", "exe", "dll", "so", "dylib", "jar", "war", "ear",
            "sql", "md", "svg"
    );

    private static final Set<String> DANGEROUS_MIME_TYPES = Set.of(
            "text/html", "application/javascript", "text/javascript",
            "application/x-javascript", "image/svg+xml",
            "application/x-sh", "application/x-msdownload",
            "application/x-msdos-program", "application/x-php"
    );

    private static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "application/octet-stream")),
            Map.entry("txt", Set.of("text/plain", "application/octet-stream")),
            Map.entry("csv", Set.of("text/csv", "application/vnd.ms-excel", "text/plain", "application/octet-stream")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
            Map.entry("rar", Set.of("application/vnd.rar", "application/x-rar-compressed", "application/octet-stream")),
            Map.entry("mp4", Set.of("video/mp4", "application/octet-stream")),
            Map.entry("mp3", Set.of("audio/mpeg", "audio/mp3", "application/octet-stream"))
    );

    private FileTypePolicy() {
    }

    static boolean isSafeExtension(String extension) {
        return !extension.isBlank() && !DANGEROUS_EXTENSIONS.contains(extension);
    }

    static String requireSafeExtension(String fileName) {
        String extension = normalizeExtension(FileUtil.getExtension(fileName));
        if (extension.isBlank()) {
            throw new RuntimeException("文件必须包含扩展名");
        }
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new RuntimeException(UiMessages.message("customer.unsafeExtension", extension));
        }
        return extension;
    }

    static void validateAllowedType(String extension, Set<String> allowedTypes, String contentType) {
        if (!allowedTypes.contains(extension)) {
            throw new RuntimeException(UiMessages.message("customer.unsupportedExtension", extension));
        }
        validateMimeType(extension, contentType);
    }

    static String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    static void validateMimeType(String extension, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (DANGEROUS_MIME_TYPES.contains(normalizedContentType)) {
            throw new RuntimeException(UiMessages.message("customer.unsafeContent", normalizedContentType));
        }
        Set<String> expectedTypes = EXTENSION_MIME_TYPES.get(extension);
        if (expectedTypes == null || expectedTypes.isEmpty() || "application/octet-stream".equals(normalizedContentType)) {
            return;
        }
        if (!expectedTypes.contains(normalizedContentType)) {
            throw new RuntimeException(UiMessages.message("customer.contentMismatch", extension, normalizedContentType));
        }
    }

}
