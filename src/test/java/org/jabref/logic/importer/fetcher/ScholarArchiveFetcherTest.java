package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@FetcherTest
public class ScholarArchiveFetcherTest {
    private ScholarArchiveFetcher fetcher;
    private BibEntry bibEntry;

    @Mock
    private ImportFormatPreferences preferences;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        fetcher = new ScholarArchiveFetcher();
        bibEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Article title")
                .withField(StandardField.AUTHOR, "Sam Liu");
    }

    @Test
    public void getNameReturnsCorrectName() {
        assertEquals("ScholarArchive", fetcher.getName());
    }

    @Test
    public void getParserReturnsNonNullParser() {
        Parser parser = fetcher.getParser();
        assertEquals(Parser.class, parser.getClass());
    }

    @Test
    public void performSearchReturnsExpectedResults() throws FetcherException {
        SearchBasedParserFetcher fetcherMock = mock(SearchBasedParserFetcher.class, Answers.RETURNS_DEEP_STUBS);
        when(fetcherMock.performSearch("query")).thenReturn(Collections.singletonList(bibEntry));
        List<BibEntry> fetchedEntries = fetcher.performSearch("query");
        assertEquals(Collections.singletonList(bibEntry), fetchedEntries);
    }
}



