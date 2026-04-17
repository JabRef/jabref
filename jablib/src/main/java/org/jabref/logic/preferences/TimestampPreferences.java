package org.jabref.logic.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class TimestampPreferences {
    private final BooleanProperty addCreationDate;
    private final BooleanProperty addModificationDate;

    // legacy pre-5.3 fields for library cleanups
    private final boolean updateTimestamp;
    private final Field timestampField;
    private final String timestampFormat;

    public TimestampPreferences(boolean addCreationDate,
                                boolean addModificationDate,
                                boolean updateTimestamp,
                                Field timestampField,
                                String timestampFormat) {
        this.addCreationDate = new SimpleBooleanProperty(addCreationDate);
        this.addModificationDate = new SimpleBooleanProperty(addModificationDate);
        this.updateTimestamp = updateTimestamp;
        this.timestampField = timestampField;
        this.timestampFormat = timestampFormat;
    }

    private TimestampPreferences() {
        this(
                false,                   // addCreationDate
                false,                   // addModificationDate
                false,                   // updateTimestamp
                StandardField.TIMESTAMP, // timestampField
                "yyyy-MM-dd"             // timestampFormat, must follow ISO-8601 because: https://xkcd.com/1179/
        );
    }

    public static TimestampPreferences getDefault() {
        return new TimestampPreferences();
    }

    public TimestampPreferences setAll(TimestampPreferences preferences) {
        setAddCreationDate(preferences.shouldAddCreationDate());
        setAddModificationDate(preferences.shouldAddModificationDate());
        // legacy prefs should not be modified
        return this;
    }

    public String now() {
        // Milli-, Micro-, and Nanoseconds are not relevant to us, so we remove them
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public boolean shouldAddCreationDate() {
        return addCreationDate.get();
    }

    public BooleanProperty addCreationDateProperty() {
        return addCreationDate;
    }

    public void setAddCreationDate(boolean addCreationDate) {
        this.addCreationDate.set(addCreationDate);
    }

    public boolean shouldAddModificationDate() {
        return addModificationDate.get();
    }

    public BooleanProperty addModificationDateProperty() {
        return addModificationDate;
    }

    public void setAddModificationDate(boolean addModificationDate) {
        this.addModificationDate.set(addModificationDate);
    }

    /// Required for migration only.
    @Deprecated
    public boolean shouldUpdateTimestamp() {
        return updateTimestamp;
    }

    /// Required for migration only.
    @Deprecated
    public Field getTimestampField() {
        return timestampField;
    }

    /// Required for migration only.
    @Deprecated
    public String getTimestampFormat() {
        return timestampFormat;
    }
}
