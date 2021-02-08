package org.jabref.logic.util;

import java.util.Collection;
import java.util.Optional;

import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class UpdateField {

    private UpdateField() {
    }

    /**
     * Updating a field will result in the entry being reformatted on save
     *
     * @param be       BibEntry
     * @param field    Field name
     * @param newValue New field value
     */
    public static Optional<FieldChange> updateField(BibEntry be, Field field, String newValue) {
        return updateField(be, field, newValue, false);
    }

    /**
     * Updating a non-displayable field does not result in the entry being reformatted on save
     *
     * @param be       BibEntry
     * @param field    Field name
     * @param newValue New field value
     */
    public static Optional<FieldChange> updateNonDisplayableField(BibEntry be, Field field, String newValue) {
        boolean changed = be.hasChanged();
        Optional<FieldChange> fieldChange = updateField(be, field, newValue, false);
        be.setChanged(changed);
        return fieldChange;
    }

    /**
     * Undoable change of field value
     *
     * @param be                        BibEntry
     * @param field                     Field name
     * @param newValue                  New field value
     * @param nullFieldIfValueIsTheSame If true the field value is removed when the current value is equals to newValue
     */
    public static Optional<FieldChange> updateField(BibEntry be, Field field, String newValue,
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
                                          OwnerPreferences ownerPreferences, TimestampPreferences timestampPreferences) {
        String defaultOwner = ownerPreferences.getDefaultOwner();
        String timestamp = timestampPreferences.now();
        boolean setOwner = ownerPreferences.isUseOwner() && (overwriteOwner || (!entry.hasField(StandardField.OWNER)));
        boolean setTimeStamp = timestampPreferences.shouldAddCreationDate();

        setAutomaticFields(entry, setOwner, defaultOwner, setTimeStamp, timestamp);
    }

    private static void setAutomaticFields(BibEntry entry, boolean setOwner, String owner, boolean setTimeStamp, String timeStamp) {

        // Set owner field if this option is enabled:
        if (setOwner) {
            // Set owner field to default value
            entry.setField(StandardField.OWNER, owner);
        }

        if (setTimeStamp) {
            entry.setField(StandardField.CREATIONDATE, timeStamp);
        }
    }

    /**
     * Sets empty or non-existing owner fields of bibtex entries inside a List to a specified default value. Timestamp
     * field is also set. Preferences are checked to see if these options are enabled.
     *
     * @param bibs List of bibtex entries
     */
    public static void setAutomaticFields(Collection<BibEntry> bibs, boolean overwriteOwner,
                                          OwnerPreferences ownerPreferences, TimestampPreferences timestampPreferences) {

        boolean globalSetOwner = ownerPreferences.isUseOwner();
        boolean setTimeStamp = timestampPreferences.shouldAddCreationDate();

        // Do not need to do anything if all options are disabled
        if (!(globalSetOwner || setTimeStamp)) {
            return;
        }

        String defaultOwner = ownerPreferences.getDefaultOwner();
        String timestamp = timestampPreferences.now();

        // Iterate through all entries
        for (BibEntry curEntry : bibs) {
            boolean setOwner = globalSetOwner && (overwriteOwner || (!curEntry.hasField(StandardField.OWNER)));
            setAutomaticFields(curEntry, setOwner, defaultOwner, setTimeStamp, timestamp);
        }
    }

    public static void setAutomaticFields(Collection<BibEntry> bibs,
                                          OwnerPreferences ownerPreferences, TimestampPreferences timestampPreferences) {
        UpdateField.setAutomaticFields(bibs,
                ownerPreferences.isOverwriteOwner(),
                ownerPreferences,
                timestampPreferences);
    }
}
