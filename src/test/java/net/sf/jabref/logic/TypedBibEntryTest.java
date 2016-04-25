package net.sf.jabref.logic;

import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Assert;
import org.junit.Test;

public class TypedBibEntryTest {
    @Test
    public void hasAllRequiredFieldsFail() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        Assert.assertFalse(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFields() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        e.setField("year", "2015");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        Assert.assertTrue(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFieldsForUnknownTypeReturnsTrue() {
        BibEntry e = new BibEntry("id", "articlllleeeee");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        Assert.assertTrue(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void getTypeForDisplayReturnsTypeName() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.INPROCEEDINGS.getName());

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        Assert.assertEquals("InProceedings", typedEntry.getTypeForDisplay());
    }

    @Test
    public void getTypeForDisplayForUnknownTypeCapitalizeFirstLetter() {
        BibEntry e = new BibEntry("id", "articlllleeeee");

        TypedBibEntry typedEntry = new TypedBibEntry(e, BibDatabaseMode.BIBTEX);
        Assert.assertEquals("Articlllleeeee", typedEntry.getTypeForDisplay());
    }
}
