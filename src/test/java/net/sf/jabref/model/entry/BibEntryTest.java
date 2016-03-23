package net.sf.jabref.model.entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BibEntryTest {
    private BibEntry entry;

    @Before
    public void setUp() {
        entry = new BibEntry();
    }

    @After
    public void tearDown() {
        entry = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void notOverrideReservedFields() {
        entry.setField(BibEntry.ID_FIELD, "somevalue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notClearReservedFields() {
        entry.clearField(BibEntry.ID_FIELD);
    }
}