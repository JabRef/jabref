package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jabref.model.entry.field.Field;

public class TimestampPreferences {
    private final boolean useTimestamps;
    private final boolean updateTimestamp;
    private final Field timestampField;
    private final String timestampFormat;
    private final boolean overwriteTimestamp;

    public TimestampPreferences(boolean useTimestamps, boolean updateTimestamp, Field timestampField, String timestampFormat, boolean overwriteTimestamp) {
        this.useTimestamps = useTimestamps;
        this.updateTimestamp = updateTimestamp;
        this.timestampField = timestampField;
        this.timestampFormat = timestampFormat;
        this.overwriteTimestamp = overwriteTimestamp;
    }

    public boolean isUseTimestamps() {
        return useTimestamps;
    }

    public boolean isUpdateTimestamp() {
        return updateTimestamp;
    }

    public Field getTimestampField() {
        return timestampField;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public boolean isOverwriteTimestamp() {
        return overwriteTimestamp;
    }

    public boolean includeTimestamps() {
        return useTimestamps && updateTimestamp;
    }

    public String now() {
        return DateTimeFormatter.ofPattern(timestampFormat).format(LocalDateTime.now());
    }
}
