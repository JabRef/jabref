package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class TypedBibEntryTest {
    @Test
    public void hasAllRequiredFieldsFail() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");

        TypedBibEntry typedEntry = new TypedBibEntry(e, Optional.empty());
        Assert.assertFalse(typedEntry.hasAllRequiredFields());
    }

    @Test
    public void hasAllRequiredFields() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        e.setField("year", "2015");

        TypedBibEntry typedEntry = new TypedBibEntry(e, Optional.empty());
        Assert.assertTrue(typedEntry.hasAllRequiredFields());
    }
}