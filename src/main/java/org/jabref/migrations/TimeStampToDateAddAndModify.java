package org.jabref.migrations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

/**
 * This class handles the migration from timestamp field to date-added and date-modified fields
 */
public class TimeStampToDateAddAndModify implements PostOpenMigration {

    private final boolean interpretTimeStampAsModificationDate;

    public TimeStampToDateAddAndModify(TimestampPreferences timestampPreferences) {
        interpretTimeStampAsModificationDate = timestampPreferences.isUpdateTimestamp();
    }

    /**
     * Handles the migration for opened databases.
     * If the old updateTimestamp setting is enabled, the timestamp field for each entry are migrated to the date-modified field.
     * Otherwise it is migrated to the date-added field.
     *
     * @param parserResult entries to be migrated
     */
    @Override
    public void performMigration(ParserResult parserResult) {
        parserResult.getDatabase().getEntries().forEach(this::migrateEntry);
    }

    private void migrateEntry(BibEntry entry) {
        entry.getField(StandardField.TIMESTAMP).ifPresent(timeStamp -> {
            String formattedTimeStamp = formatTimeStamp(timeStamp);
            if (interpretTimeStampAsModificationDate) {
                entry.setField(StandardField.MODIFICATIONDATE, formattedTimeStamp);
            } else {
                entry.setField(StandardField.CREATIONDATE, formattedTimeStamp);
            }
            entry.clearField(StandardField.TIMESTAMP);
        });
    }

    /**
     * Formats the time stamp into the local date and time format.
     * If
     * @param timeStamp
     * @return
     */
    private String formatTimeStamp(String timeStamp) {
        Optional<Date> parsedDate = Date.parse(timeStamp);
        if (parsedDate.isEmpty()) {
            // What to do if the date cannot be parsed? Do we need the custom date format possibly?
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            Date date = parsedDate.get();
            int year = date.getYear().orElse(LocalDate.now().getYear());
            int month = getMonth(date);
            int day = date.getDay().orElse(LocalDate.now().getDayOfMonth());
            LocalDateTime localDateTime = LocalDateTime.of(year, month, day, 0, 0);
            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    /**
     * Returns the month value of the passed date if available.
     * Otherwise returns the current month
     */
    private int getMonth(Date date) {
        if (date.getMonth().isPresent()) {
            return date.getMonth().get().getNumber();
        }
        return LocalDate.now().getMonthValue();
    }
}
