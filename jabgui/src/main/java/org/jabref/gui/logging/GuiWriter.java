package org.jabref.gui.logging;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.AbstractFormatPatternWriter;

public class GuiWriter extends AbstractFormatPatternWriter {

    public GuiWriter(final Map<String, String> properties) {
        super(properties);
    }

    public GuiWriter() {
        this(Map.of());
    }

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.allOf(LogEntryValue.class);
    }

    @Override
    public void write(LogEntry logEntry) {
        LogMessages.getInstance().add(logEntry);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
