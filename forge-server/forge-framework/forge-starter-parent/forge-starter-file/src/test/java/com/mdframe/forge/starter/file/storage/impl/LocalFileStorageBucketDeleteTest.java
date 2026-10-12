package com.mdframe.forge.starter.file.storage.impl;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Mock filesystem calls only: no directory creation, file deletion or real storage access. */
class LocalFileStorageBucketDeleteTest {
    private final Path base = Path.of("auth005-uncreated-storage").toAbsolutePath().normalize();

    @BeforeAll
    static void initializeMessagesBeforeMockingFilesystem() {
        UiMessages.message("error.forbidden");
    }

    @Test
    void emptyDirectoryUsesOnlyNonRecursiveDelete() throws Exception {
        LocalFileStorage storage = storage();
        Path target = base.resolve("approved");
        try (MockedStatic<Files> filesystem = mockStatic(Files.class)) {
            filesystem.when(() -> Files.deleteIfExists(target)).thenReturn(true);
            assertThat(storage.deleteBucket("approved")).isTrue();
            filesystem.verify(() -> Files.deleteIfExists(target));
        }
    }

    @Test
    void nonEmptyBusinessDirectoryRemainsUntouchedAndReturnsFailure() throws Exception {
        LocalFileStorage storage = storage();
        Path target = base.resolve("business");
        try (MockedStatic<Files> filesystem = mockStatic(Files.class)) {
            filesystem.when(() -> Files.deleteIfExists(target)).thenThrow(new DirectoryNotEmptyException(target.toString()));
            assertThat(storage.deleteBucket("business")).isFalse();
            filesystem.verify(() -> Files.deleteIfExists(target));
        }
    }

    @Test
    void traversalAndSymbolicLinkProtectionRunBeforeDeletion() {
        LocalFileStorage storage = storage();
        Path target = base.resolve("approved");
        try (MockedStatic<Files> filesystem = mockStatic(Files.class)) {
            assertThatThrownBy(() -> storage.deleteBucket("../outside")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> storage.deleteBucket("approved/child")).isInstanceOf(IllegalArgumentException.class);
            filesystem.when(() -> Files.exists(target, LinkOption.NOFOLLOW_LINKS)).thenReturn(true);
            filesystem.when(() -> Files.isSymbolicLink(target)).thenReturn(true);
            assertThatThrownBy(() -> storage.deleteBucket("approved")).isInstanceOf(IllegalArgumentException.class);
            filesystem.verify(() -> Files.deleteIfExists(target), never());
        }
    }

    private LocalFileStorage storage() {
        LocalFileStorage storage = new LocalFileStorage();
        ReflectionTestUtils.setField(storage, "baseDirectory", base);
        return storage;
    }
}
