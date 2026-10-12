package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Counts bytes actually consumed by storage and stops before a part exceeds the remaining budget. */
final class MultipartPartInputStream extends FilterInputStream {
    private final long limit;
    private final long totalLimit;
    private long count;

    MultipartPartInputStream(InputStream input, long limit, long totalLimit) {
        super(input);
        this.limit = limit;
        this.totalLimit = totalLimit;
    }

    long count() {
        return count;
    }

    @Override
    public int read() throws IOException {
        int value = in.read();
        if (value >= 0) {
            consumed(1);
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int requested = (int) Math.min(length, limit - count + 1);
        int read = in.read(bytes, offset, requested);
        if (read > 0) {
            consumed(read);
        }
        return read;
    }

    private void consumed(int bytes) {
        count += bytes;
        if (count > limit) {
            throw new BusinessException(413, UiMessages.message("customer.fileSize", totalLimit / 1024 / 1024));
        }
    }

    @Override
    public long skip(long bytes) throws IOException {
        long skipped = 0;
        while (skipped < bytes && read() >= 0) {
            skipped++;
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("Multipart byte accounting does not support reset");
    }
}
