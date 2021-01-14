package org.jabref.migrations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * This class handles the migration from timestamp field to creationdate and modificationdate fields.
 * <p>
 * If the old updateTimestamp setting is enabled, the timestamp field for each entry are migrated to the date-modified field.
 * Otherwise it is migrated to the date-added field.
 */
public class TimeStampToDateAddAndModify implements PostOpenMigration {

    private final boolean interpretTimeStampAsModificationDate;
    private final Field timeStampField;
    private final TimestampPreferences timestampPreferences;

    public TimeStampToDateAddAndModify(TimestampPreferences timestampPreferences) {
        this.timestampPreferences = timestampPreferences;
        interpretTimeStampAsModificationDate = timestampPreferences.shouldUpdateTimestamp();
        timeStampField = timestampPreferences.getTimestampField();
    }

    @Override
    public void performMigration(ParserResult parserResult) {
        parserResult.getDatabase().getEntries().forEach(this::migrateEntry);
    }

    private void migrateEntry(BibEntry entry) {
        // Query entries for their timestamp field entries
        entry.getField(timeStampField).ifPresent(timeStamp -> {
            String formattedTimeStamp = formatTimeStamp(timeStamp);
            if (interpretTimeStampAsModificationDate) {
                entry.setField(StandardField.MODIFICATIONDATE, formattedTimeStamp);
            } else {
                entry.setField(StandardField.CREATIONDATE, formattedTimeStamp);
            }
            entry.clearField(timeStampField);
        });
    }

    /**
     * Formats the time stamp into the local date and time format.
     * If the existing timestamp could not be parsed, the day/month/year "1" is used.
     * For the time portion 00:00:00 is used.
     */
    private String formatTimeStamp(String timeStamp) {
        Optional<Date> parsedDate = Date.parse(timeStamp);
        if (parsedDate.isEmpty()) {
            // What to do if the date cannot be parsed? Do we need the custom date format possibly?
            return timestampPreferences.now();
        } else {
            Date date = parsedDate.get();
            int year = date.getYear().orElse(1);
            int month = getMonth(date);
            int day = date.getDay().orElse(1);
            LocalDateTime localDateTime = LocalDateTime.of(year, month, day, 0, 0);
            // Remove any time unites smaller than seconds
            localDateTime.truncatedTo(ChronoUnit.SECONDS);
            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    /**
     * Returns the month value of the passed date if available.
     * Otherwise returns the current month.
     */
    private int getMonth(Date date) {
        if (date.getMonth().isPresent()) {
            return date.getMonth().get().getNumber();
        }
        return 1;
    }
}
