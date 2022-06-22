package org.jabref.logic.cleanup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * This class handles the migration from timestamp field to creationdate and modificationdate fields.
 * <p>
 * If the old updateTimestamp setting is enabled, the timestamp field for each entry are migrated to the date-modified field.
 * Otherwise it is migrated to the date-added field.
 */
public class TimeStampToModificationDate implements CleanupJob {

    private final Field timeStampField;

    public TimeStampToModificationDate(TimestampPreferences timestampPreferences) {
        timeStampField = timestampPreferences.getTimestampField();
    }

    /**
     * Formats the time stamp into the local date and time format.
     * If the existing timestamp could not be parsed, the day/month/year "1" is used.
     * For the time portion 00:00:00 is used.
     */
    private Optional<String> formatTimeStamp(String timeStamp) {
        Optional<Date> parsedDate = Date.parse(timeStamp);
        if (parsedDate.isEmpty()) {
            // In case the given timestamp could not be parsed
            return Optional.empty();
        } else {
            Date date = parsedDate.get();
            int year = date.getYear().orElse(1);
            int month = getMonth(date);
            int day = date.getDay().orElse(1);
            LocalDateTime localDateTime = LocalDateTime.of(year, month, day, 0, 0);
            // Remove any time unites smaller than seconds
            localDateTime.truncatedTo(ChronoUnit.SECONDS);
            return Optional.of(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        // Query entries for their timestamp field entries
        if (entry.getField(timeStampField).isPresent()) {
            Optional<String> formattedTimeStamp = formatTimeStamp(entry.getField(timeStampField).get());
            if (formattedTimeStamp.isEmpty()) {
                // In case the timestamp could not be parsed, do nothing to not lose data
                return Collections.emptyList();
            }
            // Setting the EventSource is necessary to circumvent the update of the modification date during timestamp migration
            entry.clearField(timeStampField, EntriesEventSource.CLEANUP_TIMESTAMP);
            List<FieldChange> changeList = new ArrayList<>();
            FieldChange changeTo;
            // Add removal of timestamp field
            changeList.add(new FieldChange(entry, StandardField.TIMESTAMP, formattedTimeStamp.get(), ""));
            entry.setField(StandardField.MODIFICATIONDATE, formattedTimeStamp.get(), EntriesEventSource.CLEANUP_TIMESTAMP);
            changeTo = new FieldChange(entry, StandardField.MODIFICATIONDATE, entry.getField(StandardField.MODIFICATIONDATE).orElse(""), formattedTimeStamp.get());
            changeList.add(changeTo);
            return changeList;
        }
        return Collections.emptyList();
    }
}
