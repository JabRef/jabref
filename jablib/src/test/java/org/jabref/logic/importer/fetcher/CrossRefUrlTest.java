package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImporterPreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class CrossRefUrlTest {

    @Test
    void urlIncludesApiKey() {
        CrossRef fetcher = new CrossRef(mock(ImporterPreferences.class));

        assertEquals("https://api.crossref.org/works?query=test&rows=1&mailto=user%40example.org", fetcher.getTestUrl("user@example.org"));
    }
}
