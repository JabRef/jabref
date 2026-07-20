package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.paging.Page;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import com.airhacks.afterburner.injection.Injector;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class ScholarApiFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    ScholarFetcher fetcher = new ScholarFetcher(importerPreferences);

    @BeforeEach
    void setUp() {
        BuildInfo buildInfo = Injector.instantiateModelOrService(BuildInfo.class);
        fetcher = new ScholarFetcher(importerPreferences);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(fetcher.getName())).thenReturn(Optional.of(buildInfo.scholarApiKey));
    }

    @Test
    void scholarApiJsonToBibtex() throws ParseException {
        String jsonString = """
                {\r
                          "id": "846a45f",\r
                          "title": "Paraneoplastic pemphigus case study",\r
                          "authors": ["E. R. Novak"],\r
                          "abstract": "Case report on autoimmune blistering disorders...",\r
                          "journal": "Clinical Immunology",\r
                          "journal_issn": ["1521-6616"],\r
                          "journal_issue": "3",\r
                          "journal_pages": "12-18",\r
                          "doi": "10.1016/j.clim.2023.109245",\r
                          "published_date": "2023-09-14",\r
                          "published_date_raw": "2023-09-14",\r
                          "indexed_at": "2024-03-01T12:30:45.123Z",\r
                          "has_text": true,\r
                          "has_pdf": true,\r
                          "url": "https://clinical.example.com/paper/846a45f"\r
                }""";

        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = ScholarFetcher.jsonItemToBibEntry(jsonObject);

        assertEquals(Optional.of("846a45f"), bibEntry.getField(new UnknownField("scholarapi-id")));
        assertEquals(Optional.of("2023-09-14"), bibEntry.getField(StandardField.DATE));
        assertEquals(Optional.of("2023"), bibEntry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Paraneoplastic pemphigus case study"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("E. R. Novak"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Case report on autoimmune blistering disorders..."), bibEntry.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Clinical Immunology"), bibEntry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("1521-6616"), bibEntry.getField(StandardField.ISSN));
        assertEquals(Optional.of("3"), bibEntry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("12-18"), bibEntry.getField(StandardField.PAGES));
        assertEquals(Optional.of("10.1016/j.clim.2023.109245"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("https://clinical.example.com/paper/846a45f"), bibEntry.getField(StandardField.URL));
    }

    @Test
    void performRawSearchQueryPagedWithBlankQueryReturnsEmptyPage() throws FetcherException {
        Page<BibEntry> result = fetcher.performRawSearchQueryPaged("", 0);
        assertEquals(List.of(), new ArrayList<>(result.getContent()));
    }

    @Test
    void searchByEmptyQueryFindsNothing() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @Test
    @Disabled("ScholarAPI has no journal scoped search")
    @Override
    public void supportsJournalSearch() {
    }

    @Test
    @Disabled("ScholarAPI has no author scoped search")
    @Override
    public void supportsAuthorSearch() {
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of();
    }

    @Override
    public String getTestJournal() {
        return "";
    }

    @Test
    @Override
    @DisabledOnCIServer("Unstable on CI")
    public void pageSearchReturnsUniqueResultsPerPage() {
        // Implementation is done in the interface
    }
}
