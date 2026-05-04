package org.jabref.logic.integrity;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public abstract class IntegrityCheckResultWriter implements Closeable {

    protected final List<IntegrityMessage> messages;
    protected final Writer writer;

    /// Writer lifecycle: The caller is responsible for closing the writer at the appropriate time.
    public IntegrityCheckResultWriter(Writer writer, List<IntegrityMessage> messages) {
        this.writer = writer;
        this.messages = messages;
    }

    public abstract void writeFindings() throws IOException;

    @Override
    public void close() throws IOException {
    }
}
