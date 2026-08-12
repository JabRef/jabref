package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class CrossRefUrlTest {

    @Test
    void addsConfiguredEmailToEntrySearchUrl() throws URISyntaxException, MalformedURLException {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKey(CrossRef.FETCHER_NAME)).thenReturn(Optional.of("user@example.org"));
        CrossRef fetcher = new CrossRef(importerPreferences);
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "A title");

        URL url = fetcher.getURLForEntry(entry);

        assertEquals("query.bibliographic=A%20title&rows=20&offset=0&mailto=user%40example.org", url.getQuery());
    }

    @Test
    void omitsMailtoParameterWithoutConfiguredEmail() throws URISyntaxException, MalformedURLException {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKey(CrossRef.FETCHER_NAME)).thenReturn(Optional.empty());
        CrossRef fetcher = new CrossRef(importerPreferences);
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "A title");

        URL url = fetcher.getURLForEntry(entry);

        assertEquals("query.bibliographic=A%20title&rows=20&offset=0", url.getQuery());
    }

    @Test
    void urlIncludesApiKey() {
        CrossRef fetcher = new CrossRef(mock(ImporterPreferences.class));

        assertEquals("https://api.crossref.org/works?query=test&rows=1&mailto=user%40example.org", fetcher.getTestUrl("user@example.org"));
    }
}
