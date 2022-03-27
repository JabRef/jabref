package org.jabref.model;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FieldChangeTest {

    private BibEntry entry = new BibEntry()
            .withField(StandardField.DOI, "foo");
    private BibEntry entryOther = new BibEntry();
    private FieldChange fc = new FieldChange(entry, StandardField.DOI, "foo", "bar");

    @Test
    void fieldChangeOnNullEntryNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(null, StandardField.DOI, "foo", "bar"));
    }

    @Test
    void fieldChangeOnNullFieldNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(entry, null, "foo", "bar"));
    }

    @Test
    void blankFieldChangeNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(null, null, null, null));
    }

    @Test
    void equalFieldChange() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", null);
        assertNotEquals(fc, fcBlankNewValue);
    }

    @Test
    void selfEqualsFieldchangeSameParameters() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", "bar");
        assertTrue(fc.equals(fcBlankNewValue));
    }

    @Test
    void selfEqualsFieldchangeDifferentNewValue() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", null);
        assertFalse(fc.equals(fcBlankNewValue));
    }

    @Test
    void selfEqualsFieldchangeDifferentOldValue() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, null, "bar");
        assertFalse(fc.equals(fcBlankNewValue));
    }

    @Test
    void selfEqualsFieldchangeDifferentEntry() {
        FieldChange fcBlankNewValue = new FieldChange(entryOther, StandardField.DOI, "foo", "bar");
        assertFalse(fc.equals(fcBlankNewValue));
    }

    @Test
    void fieldChangeDoesNotEqualString() {
        assertNotEquals(fc, "foo");
    }

    @Test
    void fieldChangeEqualsItSelf() {
        assertEquals(fc, fc);
    }

    @Test
    void differentFieldChangeIsNotEqual() {
        FieldChange fcOther = new FieldChange(entryOther, StandardField.DOI, "fooX", "barX");
        assertNotEquals(fc, fcOther);
    }
}
