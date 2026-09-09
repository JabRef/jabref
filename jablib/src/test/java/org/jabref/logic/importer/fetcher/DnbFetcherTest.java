package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExternalServicesTest
public class DnbFetcherTest {

    DnbFetcher dnbFetcher;
    private BibEntry bibEntryISBN9783755300274;
    @BeforeEach
    void setUp() {
        dnbFetcher = new DnbFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
                bibEntryISBN9783755300274 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.ADDRESS, "Königswinter")
                .withField(StandardField.YEAR, "2050")
                .withField(StandardField.ISBN, "3755300273")
                .withField(StandardField.SUBTITLE, "Illustrierte Ausgabe")
                .withField(StandardField.PAGETOTAL, "576")
                .withField(StandardField.AUTHOR, "Goethe, Johann Wolfgang von")
                .withField(StandardField.TITLEADDON, "Johann Wolfgang von Goethe")
                .withField(StandardField.KEYWORDS, "(Produktform)Hardback, Goethe, Weimarer Klassik, Literaturklassiker, Klassiker der Weltliteratur, Sturm und Drang, Urfaust, Faust I, Faust II, Tragödie, Heinrich Faust, Margarete, Gretchen, Valentin, Mephistopheles, Mephisto, (VLB-WN)1111: Hardcover, Softcover / Belletristik/Hauptwerk vor 1945")
                .withField(StandardField.PUBLISHER, "{Petersberg Verlag}")
                .withField(StandardField.TITLE, "Faust I, II und Urfaust");
    }

    @Test
    void getName() {
        assertEquals("DNB", dnbFetcher.getName());
    }

    @Test
    void performSearchByIdReturnsEntryForKnownIsbn() throws FetcherException {
        Optional<BibEntry> entry = dnbFetcher.performSearchById("9783755300274");
        assertEquals(Optional.of(bibEntryISBN9783755300274), entry);
    }

    @Test
    void performSearchFindsResultsForAuthorQuery() throws FetcherException {
        List<BibEntry> entries = dnbFetcher.performSearch("author=Goethe");
        assertFalse(entries.isEmpty());
    }

    @Test
    void performSearchEmpty() throws FetcherException {
        List<BibEntry> searchResults = dnbFetcher.performSearch("");
        assertEquals(List.of(), searchResults);
    }

    @Test
    void authorQueryUsesCorrectIndex() throws MalformedURLException, URISyntaxException {
        URL url = dnbFetcher.getURLForQuery(SearchBasedFetcher.getQueryNode("author=Goethe"), 0);
        assertTrue(url.toString().contains("atr%3DGoethe"));
    }

    @Test
    void startRecordCorrectForFirstAndSecondPage() throws FetcherException, MalformedURLException, URISyntaxException {
        URL page0 = dnbFetcher.getURLForQuery(SearchBasedFetcher.getQueryNode("author=Goethe"), 0);
        URL page1 = dnbFetcher.getURLForQuery(SearchBasedFetcher.getQueryNode("author=Goethe"), 1);
        assertTrue(page0.toString().contains("startRecord=1&"));
        assertTrue(page1.toString().contains("startRecord=" + (dnbFetcher.getPageSize() + 1)));
    }
}
