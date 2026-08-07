package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class UnpaywallFetcherTest {

    @Test
    void findFullText() throws IOException, FetcherException {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKey(UnpaywallFetcher.FETCHER_NAME)).thenReturn(Optional.of("test@example.org"));
        UnpaywallFetcher fetcher = new UnpaywallFetcher(importerPreferences);
        Optional<URL> actual = fetcher.findFullText(new BibEntry().withField(StandardField.DOI, "10.47397/tb/44-3/tb138kopp-jabref"));
        // Should be https://tug.org/TUGboat/tb44-3/tb138kopp-jabref.pdf - request for fix submitted to Unpaywall on 2025-11-17
        assertEquals(Optional.of(URLUtil.create("https://tug.org/TUGboat/tb41-2/tb128veytsman-overleaf.pdf")), actual);
    }
}
