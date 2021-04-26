package org.jabref.logic.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateFieldTest {

    private BibEntry entry;

    @BeforeEach
    public void setUp() throws Exception {
        entry = new BibEntry();
        entry.setChanged(false);
    }

    @Test
    public void testUpdateFieldWorksEmptyField() {
        assertFalse(entry.hasField(StandardField.YEAR));
        UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertEquals(Optional.of("2016"), entry.getField(StandardField.YEAR));
    }

    @Test
    public void testUpdateFieldWorksNonEmptyField() {
        entry.setField(StandardField.YEAR, "2015");
        UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertEquals(Optional.of("2016"), entry.getField(StandardField.YEAR));
    }

    @Test
    public void testUpdateFieldHasChanged() {
        assertFalse(entry.hasChanged());
        UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertTrue(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldValidFieldChange() {
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldCorrectFieldChangeContentsEmptyField() {
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertNull(change.get().getOldValue());
        assertEquals(StandardField.YEAR, change.get().getField());
        assertEquals("2016", change.get().getNewValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldCorrectFieldChangeContentsNonEmptyField() {
        entry.setField(StandardField.YEAR, "2015");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertEquals("2015", change.get().getOldValue());
        assertEquals(StandardField.YEAR, change.get().getField());
        assertEquals("2016", change.get().getNewValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldSameValueNoChange() {
        entry.setField(StandardField.YEAR, "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertFalse(change.isPresent());
    }

    @Test
    public void testUpdateFieldSameValueNotChange() {
        entry.setField(StandardField.YEAR, "2016");
        entry.setChanged(false);
        UpdateField.updateField(entry, StandardField.YEAR, "2016");
        assertFalse(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldSetToNullClears() {
        entry.setField(StandardField.YEAR, "2016");
        UpdateField.updateField(entry, StandardField.YEAR, null);
        assertFalse(entry.hasField(StandardField.YEAR));
    }

    @Test
    public void testUpdateFieldSetEmptyToNullClears() {
        UpdateField.updateField(entry, StandardField.YEAR, null);
        assertFalse(entry.hasField(StandardField.YEAR));
    }

    @Test
    public void testUpdateFieldSetToNullHasFieldChangeContents() {
        entry.setField(StandardField.YEAR, "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, null);
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldSetRmptyToNullHasNoFieldChangeContents() {
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, null);
        assertFalse(change.isPresent());
    }

    @Test
    public void testUpdateFieldSetToNullCorrectFieldChangeContents() {
        entry.setField(StandardField.YEAR, "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, null);
        assertNull(change.get().getNewValue());
        assertEquals(StandardField.YEAR, change.get().getField());
        assertEquals("2016", change.get().getOldValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldSameContentClears() {
        entry.setField(StandardField.YEAR, "2016");
        UpdateField.updateField(entry, StandardField.YEAR, "2016", true);
        assertFalse(entry.hasField(StandardField.YEAR));
    }

    @Test
    public void testUpdateFieldSameContentHasChanged() {
        entry.setField(StandardField.YEAR, "2016");
        entry.setChanged(false);
        UpdateField.updateField(entry, StandardField.YEAR, "2016", true);
        assertTrue(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldSameContentHasFieldChange() {
        entry.setField(StandardField.YEAR, "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016", true);
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldSameContentHasCorrectFieldChange() {
        entry.setField(StandardField.YEAR, "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, StandardField.YEAR, "2016", true);
        assertNull(change.get().getNewValue());
        assertEquals(StandardField.YEAR, change.get().getField());
        assertEquals("2016", change.get().getOldValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateNonDisplayableFieldUpdates() {
        assertFalse(entry.hasField(StandardField.YEAR));
        UpdateField.updateNonDisplayableField(entry, StandardField.YEAR, "2016");
        assertTrue(entry.hasField(StandardField.YEAR));
        assertEquals(Optional.of("2016"), entry.getField(StandardField.YEAR));
    }

    @Test
    public void testUpdateNonDisplayableFieldHasNotChanged() {
        assertFalse(entry.hasChanged());
        UpdateField.updateNonDisplayableField(entry, StandardField.YEAR, "2016");
        assertFalse(entry.hasChanged());
    }

    @Test
    public void emptyOwnerFieldNowPresentAfterAutomaticSet() {
        assertEquals(Optional.empty(), entry.getField(StandardField.OWNER), "Owner is present");

        OwnerPreferences ownerPreferences = createOwnerPreference(true, true);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(entry, false, false, ownerPreferences, timestampPreferences);

        assertEquals(Optional.of("testDefaultOwner"), entry.getField(StandardField.OWNER), "No owner exists");
    }

    @Test
    public void ownerAssignedCorrectlyAfterAutomaticSet() {
        OwnerPreferences ownerPreferences = createOwnerPreference(true, true);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(entry, false, false, ownerPreferences, timestampPreferences);

        assertEquals(Optional.of("testDefaultOwner"), entry.getField(StandardField.OWNER));
    }

    @Test
    public void ownerIsNotResetAfterAutomaticSetIfOverwriteOwnerFalse() {
        String alreadySetOwner = "alreadySetOwner";
        entry.setField(StandardField.OWNER, alreadySetOwner);

        assertEquals(Optional.of(alreadySetOwner), entry.getField(StandardField.OWNER));

        OwnerPreferences ownerPreferences = createOwnerPreference(true, false);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(entry, false, false, ownerPreferences, timestampPreferences);

        assertNotEquals(Optional.of("testDefaultOwner"), entry.getField(StandardField.OWNER), "Owner has changed");
        assertEquals(Optional.of(alreadySetOwner), entry.getField(StandardField.OWNER), "Owner has not changed");
    }

    @Test
    public void emptyCreationdateFieldNowPresentAfterAutomaticSet() {
        assertEquals(Optional.empty(), entry.getField(StandardField.CREATIONDATE), "CreationDate is present");

        OwnerPreferences ownerPreferences = createOwnerPreference(true, true);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(entry, false, false, ownerPreferences, timestampPreferences);

        String creationDate = timestampPreferences.now();

        assertEquals(Optional.of(creationDate), entry.getField(StandardField.CREATIONDATE), "No CreationDate exists");
    }

    @Test
    public void creationdateAssignedCorrectlyAfterAutomaticSet() {
        OwnerPreferences ownerPreferences = createOwnerPreference(true, true);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(entry, false, false, ownerPreferences, timestampPreferences);

        String creationDate = timestampPreferences.now();

        assertEquals(Optional.of(creationDate), entry.getField(StandardField.CREATIONDATE), "Not the same date");
    }

    @Test
    public void ownerSetToDefaultValueForCollectionOfBibEntries() {
        BibEntry entry2 = new BibEntry();
        BibEntry entry3 = new BibEntry();

        assertEquals(Optional.empty(), entry.getField(StandardField.OWNER), "Owner field for entry is present");
        assertEquals(Optional.empty(), entry.getField(StandardField.OWNER), "Owner field for entry2 is present");
        assertEquals(Optional.empty(), entry.getField(StandardField.OWNER), "Owner field for entry3 is present");

        Collection<BibEntry> bibs = Arrays.asList(entry, entry2, entry3);

        OwnerPreferences ownerPreferences = createOwnerPreference(true, true);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(bibs, false, ownerPreferences, timestampPreferences);

        String defaultOwner = "testDefaultOwner";

        assertEquals(Optional.of(defaultOwner), entry.getField(StandardField.OWNER), "entry has no owner field");
        assertEquals(Optional.of(defaultOwner), entry2.getField(StandardField.OWNER), "entry2 has no owner field");
        assertEquals(Optional.of(defaultOwner), entry3.getField(StandardField.OWNER), "entry3 has no owner field");
    }

    @Test
    public void ownerNotChangedForCollectionOfBibEntriesIfOptionsDisabled() {
        String initialOwner = "initialOwner";

        entry.setField(StandardField.OWNER, initialOwner);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.OWNER, initialOwner);

        assertEquals(Optional.of(initialOwner), entry.getField(StandardField.OWNER), "Owner field for entry is not present");
        assertEquals(Optional.of(initialOwner), entry2.getField(StandardField.OWNER), "Owner field for entry2 is not present");

        Collection<BibEntry> bibs = Arrays.asList(entry, entry2);

        OwnerPreferences ownerPreferences = createOwnerPreference(true, false);
        TimestampPreferences timestampPreferences = createTimestampPreference();
        UpdateField.setAutomaticFields(bibs, false, ownerPreferences, timestampPreferences);

        assertEquals(Optional.of(initialOwner), entry.getField(StandardField.OWNER), "entry has new value for owner field");
        assertEquals(Optional.of(initialOwner), entry2.getField(StandardField.OWNER), "entry2 has new value for owner field");
    }

    private OwnerPreferences createOwnerPreference(boolean useOwner, boolean overwriteOwner) {
        String defaultOwner = "testDefaultOwner";
        return new OwnerPreferences(useOwner, defaultOwner, overwriteOwner);
    }

    private TimestampPreferences createTimestampPreference() {
        return new TimestampPreferences(true, true, true, StandardField.CREATIONDATE, "dd.mm.yyyy");
    }
}
