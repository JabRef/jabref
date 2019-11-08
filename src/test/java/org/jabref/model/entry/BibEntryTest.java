package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contains misc tests for BibEntry especially not using the default constructor: {@link
 * BibEntryWithDefaultConstructorTest}
 */
class BibEntryTest {
    @Test
    public void allFieldsPresentDefault() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.AUTHOR));
        requiredFields.add(new OrFields(StandardField.TITLE));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void allFieldsPresentOr() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.JOURNAL, StandardField.YEAR));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR, StandardField.ADDRESS));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void isNullCiteKeyThrowsNPE() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertThrows(NullPointerException.class, () -> e.setCiteKey(null));
    }

    @Test
    public void isEmptyCiteKey() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertFalse(e.hasCiteKey());

        e.setCiteKey("");
        assertFalse(e.hasCiteKey());

        e.setCiteKey("key");
        assertTrue(e.hasCiteKey());

        e.clearField(InternalField.KEY_FIELD);
        assertFalse(e.hasCiteKey());
    }
}
