package org.jabref.logic.util;

import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
