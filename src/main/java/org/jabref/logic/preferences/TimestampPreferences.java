package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jabref.model.entry.field.Field;

public class TimestampPreferences {
    private final boolean addCreationDate;
    private final boolean addModificationDate;
    private final Field timestampField;
    private final String timestampFormat;

    // Old settings used for migration
    private final boolean updateTimestamp;

    public TimestampPreferences(boolean addCreationDate, boolean modifyTimestamp, boolean updateTimestamp, Field timestampField, String timestampFormat) {
        this.addCreationDate = addCreationDate;
        this.addModificationDate = modifyTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.timestampField = timestampField;
        this.timestampFormat = timestampFormat;
    }

    public String now() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public boolean shouldAddCreationDate() {
        return addCreationDate;
    }

    public boolean shouldAddModificationDate() {
        return addModificationDate;
    }

    public boolean shouldUpdateTimestamp() {
        return updateTimestamp;
    }

    public Field getTimestampField() {
        return timestampField;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }
}
