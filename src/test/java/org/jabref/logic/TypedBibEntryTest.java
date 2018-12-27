package org.jabref.logic;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypedBibEntryTest {

    @Test
    public void hasAllRequiredFieldsFail() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertFalse(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFields() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        e.setField("year", "2015");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertTrue(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFieldsForUnknownTypeReturnsTrue() {
        BibEntry e = new BibEntry("articlllleeeee");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertTrue(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void getTypeForDisplayReturnsTypeName() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertEquals("InProceedings", typedEntry.getTypeForDisplay());
    }

    @Test
    public void getTypeForDisplayForUnknownTypeCapitalizeFirstLetter() {
        BibEntry e = new BibEntry("articlllleeeee");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        assertEquals("Articlllleeeee", typedEntry.getTypeForDisplay());
    }
}
