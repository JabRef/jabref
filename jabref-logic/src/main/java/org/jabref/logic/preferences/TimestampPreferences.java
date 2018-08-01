package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampPreferences {
    private final boolean useTimestamps;
    private final boolean useModifiedTimestamp;
    private final String timestampField;
    private final String timestampFormat;
    private final boolean overwriteTimestamp;

    public TimestampPreferences(boolean useTimestamps, boolean useModifiedTimestamp, String timestampField, String timestampFormat, boolean overwriteTimestamp) {
        this.useTimestamps = useTimestamps;
        this.useModifiedTimestamp = useModifiedTimestamp;
        this.timestampField = timestampField;
        this.timestampFormat = timestampFormat;
        this.overwriteTimestamp = overwriteTimestamp;
    }

    public boolean includeCreatedTimestamp() {
        return useTimestamps;
    }

    public boolean includeModifiedTimestamp() {
        return useModifiedTimestamp;
    }

    public String getTimestampField() {
        return timestampField;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public boolean overwriteTimestamp() {
        return overwriteTimestamp;
    }

    public boolean includeTimestamps() {
        return useTimestamps && useModifiedTimestamp;
    }

    public String now() {
        String timeStampFormat = timestampFormat;
        return DateTimeFormatter.ofPattern(timeStampFormat).format(LocalDateTime.now());
    }
}
