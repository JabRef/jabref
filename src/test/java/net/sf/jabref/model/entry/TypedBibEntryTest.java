package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibDatabaseMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class TypedBibEntryTest {
    @Test
    public void hasAllRequiredFieldsFail() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");

        TypedBibEntry typedEntry = new TypedBibEntry(e, Optional.empty(), BibDatabaseMode.BIBTEX);
        Assert.assertFalse(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFields() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        e.setField("year", "2015");

        TypedBibEntry typedEntry = new TypedBibEntry(e, Optional.empty(), BibDatabaseMode.BIBTEX);
        Assert.assertTrue(typedEntry.hasAllRequiredFields());
    }
}
