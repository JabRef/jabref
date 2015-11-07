package net.sf.jabref.logic.cleanup;

import junit.framework.Assert;
import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PageNumbersCleanupTest {
    private BibtexEntry entry;

    @Before
    public void setUp() {
        entry = new BibtexEntry();
    }

    @After
    public void teardown() {
        entry = null;
    }

    @Test
    public void formatPageNumbers() {
        entry.setField("pages", "1-2");
        new PageNumbersCleanup(entry).cleanup();

        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void onlyFormatPageNumbersField() {
        entry.setField("otherfield", "1-2");
        new PageNumbersCleanup(entry).cleanup();

        Assert.assertEquals("1-2", entry.getField("otherfield"));
    }
}