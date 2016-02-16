package net.sf.jabref.model.entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BibEntryTest {
    private BibEntry entry;

    @Before
    public void setUp() throws Exception {
        entry = new BibEntry();
    }

    @After
    public void tearDown() throws Exception {
        entry = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void notOverrideReservedFields() throws Exception {
        entry.setField(BibEntry.ID_FIELD, "somevalue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notClearReservedFields() throws Exception {
        entry.clearField(BibEntry.ID_FIELD);
    }
}