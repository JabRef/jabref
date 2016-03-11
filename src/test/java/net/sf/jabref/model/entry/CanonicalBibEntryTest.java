package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

public class CanonicalBibEntryTest {

    @Test
    public void simpleCanonicalRepresentation() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField(BibEntry.KEY_FIELD, "key");
        e.setField("author", "abc");
        e.setField("title", "def");
        e.setField("journal", "hij");
        String canonicalRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(e);
        Assert.assertEquals("@article{key,\n  author = {abc},\n  journal = {hij},\n  title = {def}\n}",
                canonicalRepresentation);
    }

    @Test
    public void canonicalRepresentationWithNewlines() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField(BibEntry.KEY_FIELD, "key");
        e.setField("abstract", "line 1\nline 2");
        String canonicalRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(e);
        Assert.assertEquals("@article{key,\n  abstract = {line 1\nline 2}\n}", canonicalRepresentation);
    }

    @Test
    public void canonicalRepresentationOfAnAuthor() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        e.setField(BibEntry.KEY_FIELD, "key");
        e.setField("author", "K. Crowston and H. Annabi and J. Howison and C. Masango");
        String canonicalRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(e);
        Assert.assertEquals(
                "@article{key,\n  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}\n}",
                canonicalRepresentation);
    }
}
