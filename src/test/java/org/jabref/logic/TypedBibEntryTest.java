package org.jabref.logic;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypedBibEntryTest {
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        entryTypesManager = new BibEntryTypesManager();
    }

    @Test
    public void hasAllRequiredFieldsFail() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertFalse(typedEntry.hasAllRequiredFields(entryTypesManager));
    }

    @Test
    public void hasAllRequiredFields() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");
        e.setField(StandardField.YEAR, "2015");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertTrue(typedEntry.hasAllRequiredFields(entryTypesManager));
    }

    @Test
    public void hasAllRequiredFieldsForUnknownTypeReturnsTrue() {
        BibEntry e = new BibEntry(new UnknownEntryType("articlllleeeee"));

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertTrue(typedEntry.hasAllRequiredFields(entryTypesManager));
    }

    @Test
    public void getTypeForDisplayReturnsTypeName() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertEquals("InProceedings", typedEntry.getTypeForDisplay());
    }

    @Test
    public void getTypeForDisplayForUnknownTypeCapitalizeFirstLetter() {
        BibEntry e = new BibEntry(new UnknownEntryType("articlllleeeee"));

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertEquals("Articlllleeeee", typedEntry.getTypeForDisplay());
    }
}
