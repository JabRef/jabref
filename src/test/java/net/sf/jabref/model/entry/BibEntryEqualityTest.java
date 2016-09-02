package net.sf.jabref.model.entry;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BibEntryEqualityTest {
    @Test
    public void identicObjectsareEqual() throws Exception {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = e1;
        assertTrue(e1.equals(e2));
    }

    @Test
    public void compareToNullObjectIsFalse() throws Exception {
        BibEntry e1 = new BibEntry();
        assertFalse(e1.equals(null));
    }

    @Test
    public void compareToDifferentClassIsFalse() throws Exception {
        BibEntry e1 = new BibEntry();
        Object e2 = new Object();
        assertFalse(e1.equals(e2));
    }

    @Test
    public void compareIsTrueWhenIdAndFieldsAreEqual() throws Exception {
        BibEntry e1 = new BibEntry("1");
        e1.setField("key", "value");
        BibEntry e2 = new BibEntry("1");
        assertFalse(e1.equals(e2));
        e2.setField("key", "value");
        assertTrue(e1.equals(e2));
    }
}
