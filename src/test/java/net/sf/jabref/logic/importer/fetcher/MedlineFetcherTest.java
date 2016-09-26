package net.sf.jabref.logic.importer.fetcher;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MedlineFetcherTest {

    private MedlineFetcher fetcher;

    @Before
    public void setUp(){
        fetcher = new MedlineFetcher();
    }

    @Test
    public void testGetName(){
        assertEquals("Medline", fetcher.getName());
    }

    @Test
    public void testGetHelpPage(){
        assertEquals("Medline", fetcher.getHelpPage().getPageName());
    }
}
