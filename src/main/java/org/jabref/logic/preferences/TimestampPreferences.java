package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.jabref.model.entry.field.Field;

public class TimestampPreferences {
    private final boolean addCreationDate;
    private final boolean addModificationDate;

    // These are old preferences. They are used for migration only.
    private final boolean updateTimestamp;
    private final Field timestampField;
    private final String timestampFormat;

    public TimestampPreferences(boolean addCreationDate, boolean modifyTimestamp, boolean updateTimestamp, Field timestampField, String timestampFormat) {
        this.addCreationDate = addCreationDate;
        this.addModificationDate = modifyTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.timestampField = timestampField;
        this.timestampFormat = timestampFormat;
    }

    public String now() {
        // Milli-, Micro-, and Nanoseconds are not relevant to us, so we remove them
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public boolean shouldAddCreationDate() {
        return addCreationDate;
    }

    public boolean shouldAddModificationDate() {
        return addModificationDate;
    }

    /**
     * Required for migration only.
     */
    public boolean shouldUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Required for migration only.
     */
    public Field getTimestampField() {
        return timestampField;
    }

    /**
     * Required for migration only.
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }
}
