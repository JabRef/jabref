package net.sf.jabref.logic.util;

import java.util.Optional;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class UpdateFieldTest {

    private BibEntry entry;


    @Before
    public void setUp() throws Exception {
        entry = new BibEntry();
        entry.setChanged(false);
    }

    @Test
    public void testUpdateFieldWorksEmptyField() {
        assertFalse(entry.hasField("year"));
        UpdateField.updateField(entry, "year", "2016");
        assertEquals("2016", entry.getField("year"));
    }

    @Test
    public void testUpdateFieldWorksNonEmptyField() {
        entry.setField("year", "2015");
        UpdateField.updateField(entry, "year", "2016");
        assertEquals("2016", entry.getField("year"));
    }

    @Test
    public void testUpdateFieldHasChanged() {
        assertFalse(entry.hasChanged());
        UpdateField.updateField(entry, "year", "2016");
        assertTrue(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldValidFieldChange() {
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016");
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldCorrectFieldChangeContentsEmptyField() {
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016");
        assertNull(change.get().getOldValue());
        assertEquals("year", change.get().getField());
        assertEquals("2016", change.get().getNewValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldCorrectFieldChangeContentsNonEmptyField() {
        entry.setField("year", "2015");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016");
        assertEquals("2015", change.get().getOldValue());
        assertEquals("year", change.get().getField());
        assertEquals("2016", change.get().getNewValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldSameValueNoChange() {
        entry.setField("year", "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016");
        assertFalse(change.isPresent());
    }

    @Test
    public void testUpdateFieldSameValueNotChange() {
        entry.setField("year", "2016");
        entry.setChanged(false);
        UpdateField.updateField(entry, "year", "2016");
        assertFalse(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldSetToNullClears() {
        entry.setField("year", "2016");
        UpdateField.updateField(entry, "year", null);
        assertFalse(entry.hasField("year"));
    }

    @Test
    public void testUpdateFieldSetEmptyToNullClears() {
        UpdateField.updateField(entry, "year", null);
        assertFalse(entry.hasField("year"));
    }

    @Test
    public void testUpdateFieldSetToNullHasFieldChangeContents() {
        entry.setField("year", "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", null);
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldSetRmptyToNullHasNoFieldChangeContents() {
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", null);
        assertFalse(change.isPresent());
    }

    @Test
    public void testUpdateFieldSetToNullCorrectFieldChangeContents() {
        entry.setField("year", "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", null);
        assertNull(change.get().getNewValue());
        assertEquals("year", change.get().getField());
        assertEquals("2016", change.get().getOldValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateFieldSameContentClears() {
        entry.setField("year", "2016");
        UpdateField.updateField(entry, "year", "2016", true);
        assertFalse(entry.hasField("year"));
    }

    @Test
    public void testUpdateFieldSameContentHasChanged() {
        entry.setField("year", "2016");
        entry.setChanged(false);
        UpdateField.updateField(entry, "year", "2016", true);
        assertTrue(entry.hasChanged());
    }

    @Test
    public void testUpdateFieldSameContentHasFieldChange() {
        entry.setField("year", "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016", true);
        assertTrue(change.isPresent());
    }

    @Test
    public void testUpdateFieldSameContentHasCorrectFieldChange() {
        entry.setField("year", "2016");
        Optional<FieldChange> change = UpdateField.updateField(entry, "year", "2016", true);
        assertNull(change.get().getNewValue());
        assertEquals("year", change.get().getField());
        assertEquals("2016", change.get().getOldValue());
        assertEquals(entry, change.get().getEntry());
    }

    @Test
    public void testUpdateNonDisplayableFieldUpdates() {
        assertFalse(entry.hasField("year"));
        UpdateField.updateNonDisplayableField(entry, "year", "2016");
        assertTrue(entry.hasField("year"));
        assertEquals("2016", entry.getField("year"));
    }

    @Test
    public void testUpdateNonDisplayableFieldHasNotChanged() {
        assertFalse(entry.hasChanged());
        UpdateField.updateNonDisplayableField(entry, "year", "2016");
        assertFalse(entry.hasChanged());
    }

}
