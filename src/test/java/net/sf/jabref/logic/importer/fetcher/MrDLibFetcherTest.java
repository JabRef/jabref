package net.sf.jabref.logic.importer.fetcher;

import java.util.List;

import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.testutils.category.FetcherTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(FetcherTests.class)
public class MrDLibFetcherTest {

    private MrDLibFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new MrDLibFetcher("", "");
        bibEntry = new BibEntry();
        bibEntry.setField(FieldName.TITLE, "lernen");
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        List<BibEntry> bibEntrys = fetcher.performSearch(bibEntry);
        assertFalse(bibEntrys.isEmpty());
    }

    @Test
    public void testGetName() {
        assertEquals("MDL_FETCHER", fetcher.getName());
    }

}
