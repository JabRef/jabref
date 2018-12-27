package org.jabref.logic.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class UpdateField {

    private UpdateField() {
    }

    /**
     * Updating a field will result in the entry being reformatted on save
     *
     * @param be         BibEntry
     * @param field      Field name
     * @param newValue   New field value
     */
    public static Optional<FieldChange> updateField(BibEntry be, String field, String newValue) {
        return updateField(be, field, newValue, false);
    }

    /**
     * Updating a non-displayable field does not result in the entry being reformatted on save
     *
     * @param be         BibEntry
     * @param field      Field name
     * @param newValue   New field value
     */
    public static Optional<FieldChange> updateNonDisplayableField(BibEntry be, String field, String newValue) {
        boolean changed = be.hasChanged();
        Optional<FieldChange> fieldChange = updateField(be, field, newValue, false);
        be.setChanged(changed);
        return fieldChange;
    }

    /**
     * Undoable change of field value
     *
     * @param be                          BibEntry
     * @param field                       Field name
     * @param newValue                    New field value
     * @param nullFieldIfValueIsTheSame   If true the field value is removed when the current value is equals to newValue
     */
    public static Optional<FieldChange> updateField(BibEntry be, String field, String newValue,
            Boolean nullFieldIfValueIsTheSame) {
        String writtenValue = null;
        String oldValue = null;
        if (be.hasField(field)) {
            oldValue = be.getField(field).get();
            if ((newValue == null) || (oldValue.equals(newValue) && nullFieldIfValueIsTheSame)) {
                // If the new field value is null or the old and the new value are the same and flag is set
                // Clear the field
                be.clearField(field);
            } else if (!oldValue.equals(newValue)) {
                // Update
                writtenValue = newValue;
                be.setField(field, newValue);
            } else {
                // Values are the same, do nothing
                return Optional.empty();
            }
        } else {
            // old field value not set
            if (newValue == null) {
                // Do nothing
                return Optional.empty();
            } else {
                // Set new value
                writtenValue = newValue;
                be.setField(field, newValue);
            }
        }
        return Optional.of(new FieldChange(be, field, oldValue, writtenValue));
    }

    /**
     * Sets empty or non-existing owner fields of a bibtex entry to a specified default value. Timestamp field is also
     * set. Preferences are checked to see if these options are enabled.
     *
     * @param entry              The entry to set fields for.
     * @param overwriteOwner     Indicates whether owner should be set if it is already set.
     * @param overwriteTimestamp Indicates whether timestamp should be set if it is already set.
     */
    public static void setAutomaticFields(BibEntry entry, boolean overwriteOwner, boolean overwriteTimestamp,
            UpdateFieldPreferences prefs) {
        String defaultOwner = prefs.getDefaultOwner();
        String timestamp = DateTimeFormatter.ofPattern(prefs.getTimeStampFormat()).format(LocalDateTime.now());
        String timeStampField = prefs.getTimeStampField();
        boolean setOwner = prefs.isUseOwner() && (overwriteOwner || (!entry.hasField(FieldName.OWNER)));
        boolean setTimeStamp = prefs.isUseTimeStamp() && (overwriteTimestamp || (!entry.hasField(timeStampField)));

        setAutomaticFields(entry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
    }

    public static void setAutomaticFields(BibEntry entry, UpdateFieldPreferences prefs) {
        UpdateField.setAutomaticFields(entry, prefs.isOverwriteOwner(), prefs.isOverwriteTimeStamp(), prefs);
    }

    private static void setAutomaticFields(BibEntry entry, boolean setOwner, String owner, boolean setTimeStamp,
            String timeStampField, String timeStamp) {

        // Set owner field if this option is enabled:
        if (setOwner) {
            // Set owner field to default value
            entry.setField(FieldName.OWNER, owner);
        }

        if (setTimeStamp) {
            entry.setField(timeStampField, timeStamp);
        }
    }

    /**
     * Sets empty or non-existing owner fields of bibtex entries inside a List to a specified default value. Timestamp
     * field is also set. Preferences are checked to see if these options are enabled.
     *
     * @param bibs List of bibtex entries
     */
    public static void setAutomaticFields(Collection<BibEntry> bibs, boolean overwriteOwner,
            boolean overwriteTimestamp, UpdateFieldPreferences prefs) {

        boolean globalSetOwner = prefs.isUseOwner();
        boolean globalSetTimeStamp = prefs.isUseTimeStamp();

        // Do not need to do anything if all options are disabled
        if (!(globalSetOwner || globalSetTimeStamp)) {
            return;
        }

        String timeStampField = prefs.getTimeStampField();
        String defaultOwner = prefs.getDefaultOwner();
        String timestamp = DateTimeFormatter.ofPattern(prefs.getTimeStampFormat()).format(LocalDateTime.now());

        // Iterate through all entries
        for (BibEntry curEntry : bibs) {
            boolean setOwner = globalSetOwner && (overwriteOwner || (!curEntry.hasField(FieldName.OWNER)));
            boolean setTimeStamp = globalSetTimeStamp && (overwriteTimestamp || (!curEntry.hasField(timeStampField)));
            setAutomaticFields(curEntry, setOwner, defaultOwner, setTimeStamp, timeStampField, timestamp);
        }
    }

    public static void setAutomaticFields(Collection<BibEntry> bibs, UpdateFieldPreferences prefs) {
        UpdateField.setAutomaticFields(bibs, prefs.isOverwriteOwner(), prefs.isOverwriteTimeStamp(), prefs);
    }
}
