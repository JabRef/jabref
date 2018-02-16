package org.jabref.logic.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

/**
 * An input stream that keeps track of the amount of bytes already read.
 * Code based on http://stackoverflow.com/a/1339589/873661, but converted to use JavaFX properties instead of listeners
 */
public class ProgressInputStream extends FilterInputStream {
    private final long maxNumBytes;
    private final LongProperty totalNumBytesRead;
    private final LongProperty progress;

    public ProgressInputStream(InputStream in, long maxNumBytes) {
        super(in);
        this.maxNumBytes = maxNumBytes;
        this.totalNumBytesRead = new SimpleLongProperty(0);
        this.progress = new SimpleLongProperty(0);
        this.progress.bind(totalNumBytesRead.divide(maxNumBytes));
    }

    public long getTotalNumBytesRead() {
        return totalNumBytesRead.get();
    }

    public LongProperty totalNumBytesReadProperty() {
        return totalNumBytesRead;
    }

    public long getProgress() {
        return progress.get();
    }

    public LongProperty progressProperty() {
        return progress;
    }

    public long getMaxNumBytes() {
        return maxNumBytes;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        updateProgress(1);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return (int) updateProgress(super.read(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        return updateProgress(super.skip(n));
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private long updateProgress(long numBytesRead) {
        if (numBytesRead > 0) {
            totalNumBytesRead.set(totalNumBytesRead.get() + numBytesRead);
        }

        return numBytesRead;
    }
}
