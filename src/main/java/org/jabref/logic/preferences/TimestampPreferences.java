package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampPreferences {
    private final boolean addCreationDate;
    private final boolean addModificationDate;

    // Old settings used for migration
    private final boolean useTimestamps;
    private final boolean updateTimestamp;

    public TimestampPreferences(boolean addCreationDate, boolean modifyTimestamp, boolean useTimestamps, boolean updateTimestamp) {
        this.addCreationDate = addCreationDate;
        this.addModificationDate = modifyTimestamp;
        this.useTimestamps = useTimestamps;
        this.updateTimestamp = updateTimestamp;
    }

    public String now() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    }

    public boolean isAddCreationDate() {
        return addCreationDate;
    }

    public boolean isAddModificationDate() {
        return addModificationDate;
    }

    public boolean isUpdateTimestamp() {
        return updateTimestamp;
    }

    public boolean isUseTimestamps() {
        return useTimestamps;
    }
}
