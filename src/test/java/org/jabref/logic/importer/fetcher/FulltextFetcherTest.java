package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FulltextFetcherTest {

    private static List<FulltextFetcher> fetcherProvider() {
        return new FulltextFetchers(mock(ImportFormatPreferences.class)).getFetchers();
    }

    @ParameterizedTest
    @MethodSource("fetcherProvider")
    void findFullTextRejectsNullParameter(FulltextFetcher fetcher) {
        assertThrows(NullPointerException.class, () -> fetcher.findFullText(null));
    }

    @ParameterizedTest
    @MethodSource("fetcherProvider")
    void findFullTextWithEmptyEntryFindsNothing(FulltextFetcher fetcher) throws Exception {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }
}
