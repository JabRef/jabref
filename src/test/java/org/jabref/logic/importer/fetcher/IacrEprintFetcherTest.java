package org.jabref.logic.importer.fetcher;

import org.junit.Test;

import static org.junit.Assert.*;


public class IacrEprintFetcherTest {

    @Test
    public void testPerformSearchById() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetDate() {
        String input = "received 9 Nov 2017, last revised 10 Nov 2017";
        assertEquals("2017-11-10", IacrEprintFetcher.getDate(input));
        input = "received 9 Nov 2017";
        assertEquals("2017-11-09", IacrEprintFetcher.getDate(input));
    }

}
