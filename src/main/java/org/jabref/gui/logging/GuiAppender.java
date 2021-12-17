package org.jabref.gui.logging;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.logging.LogMessages;

import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.AbstractFormatPatternWriter;

public class GuiAppender extends AbstractFormatPatternWriter {

    public GuiAppender(final Map<String, String> properties) {
        super(properties);
    }

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.allOf(LogEntryValue.class);
    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        DefaultTaskExecutor.runInJavaFXThread(() -> LogMessages.getInstance().add(logEntry));
    }

    @Override
    public void flush() throws Exception {
    }

    @Override
    public void close() throws Exception {
    }

}