package org.jabref.model;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldChangeTest {

    private final BibEntry entry = new BibEntry()
            .withField(StandardField.DOI, "foo");
    private final BibEntry entryOther = new BibEntry();
    private final FieldChange fc = new FieldChange(entry, StandardField.DOI, "foo", "bar");

    @Test
    void equalFieldChange() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", null);
        assertNotEquals(fc, fcBlankNewValue);
    }

    @Test
    void selfEqualsFieldchangeSameParameters() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", "bar");
        assertEquals(fc, fcBlankNewValue);
    }

    @Test
    void selfEqualsFieldchangeDifferentOldValue() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, null, "bar");
        assertNotEquals(fc, fcBlankNewValue);
    }

    @Test
    void selfEqualsFieldchangeDifferentEntry() {
        FieldChange fcBlankNewValue = new FieldChange(entryOther, StandardField.DOI, "foo", "bar");
        assertNotEquals(fc, fcBlankNewValue);
    }

    @Test
    void fieldChangeDoesNotEqualString() {
        assertNotEquals("foo", fc);
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
