package org.jf.util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends InputStream {
    private int filePosition;
    @Nonnull private final RandomAccessFile raf;

    public RandomAccessFileInputStream(@Nonnull RandomAccessFile raf, int filePosition) {
        this.filePosition = filePosition;
        this.raf = raf;
    }

    @Override public int read() throws IOException {
        raf.seek(filePosition);
        filePosition++;
        return raf.read();
    }

    @Override public int read(byte[] bytes) throws IOException {
        raf.seek(filePosition);
        int bytesRead = raf.read(bytes);
        filePosition += bytesRead;
        return bytesRead;
    }

    @Override public int read(byte[] bytes, int offset, int length) throws IOException {
        raf.seek(filePosition);
        int bytesRead = raf.read(bytes, offset, length);
        filePosition += bytesRead;
        return bytesRead;
    }

    @Override public long skip(long l) throws IOException {
        int skipBytes = Math.min((int)l, available());
        filePosition += skipBytes;
        return skipBytes;
    }

    @Override public int available() throws IOException {
        return (int)raf.length() - filePosition;
    }

    @Override public boolean markSupported() {
        return false;
    }
}
