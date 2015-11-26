package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

public class CanonicalBibtexEntryTest {

    /**
     * Simple test for the canonical format
     */
    @Test
    public void canonicalRepresentation() {
        BibtexEntry e = new BibtexEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField(BibtexEntry.KEY_FIELD, "key");
        e.setField("author", "abc");
        e.setField("title", "def");
        e.setField("journal", "hij");
        String canonicalRepresentation = CanonicalBibtexEntry.getCanonicalRepresentation(e);
        Assert.assertEquals("@article{key,\n  author = {abc},\n  journal = {hij},\n  title = {def}\n}",
                canonicalRepresentation);
    }

}
