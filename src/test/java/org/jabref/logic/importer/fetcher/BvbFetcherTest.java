package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

@FetcherTest
public class BvbFetcherTest {

    BvbFetcher fetcher = new BvbFetcher();

    @Test
    void testPerformTest() throws Exception {
        List<BibEntry> result = fetcher.performSearch("Digitalisierung");

        result.forEach(entry -> System.out.println(entry.toString()));
    }
}
