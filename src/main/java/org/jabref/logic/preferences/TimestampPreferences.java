package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampPreferences {
    private final boolean addCreationDate;
    private final boolean addModificationDate;

    public TimestampPreferences(boolean addCreationDate, boolean modifyTimestamp) {
        this.addCreationDate = addCreationDate;
        this.addModificationDate = modifyTimestamp;
    }
1
    public String now() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public boolean isAddCreationDate() {
        return addCreationDate;
    }

    public boolean isAddModificationDate() {
        return addModificationDate;
    }
}
