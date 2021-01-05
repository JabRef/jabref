package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jabref.model.entry.field.Field;

public class TimestampPreferences {
    private final boolean addTimestamp;
    private final boolean modifyTimestamp;

    public TimestampPreferences(boolean addTimestamp, boolean modifyTimestamp) {
        this.addTimestamp = addTimestamp;
        this.modifyTimestamp = modifyTimestamp;
    }
1
    public String now() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public boolean isAddTimestamp() {
        return addTimestamp;
    }

    public boolean isModifyTimestamp() {
        return modifyTimestamp;
    }
}
