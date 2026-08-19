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
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
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
public class ScholarFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

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
                {
                       "id": "7184",
                       "title": "Methylated N-(4-N,N-Dimethylaminobenzyl) Chitosan, a Novel Chitosan Derivative, Enhances Paracellular Permeability Across Intestinal Epithelial Cells (Caco-2)",
                       "authors": [
                         "Jariya Kowapradit",
                         "Praneet Opanasopit",
                         "Tanasait Ngawhiranpat"
                       ],
                       "journal": "AAPS PharmSciTech",
                       "journal_publisher": "Springer International Publishing",
                       "journal_issn": [
                         "1530-9932"
                       ],
                       "journal_issue": "Volume 9, Issue 4",
                       "journal_pages": "1143-1152",
                       "doi": "10.1208/s12249-008-9160-7",
                       "published_date": "2008-12-01T00:00:00Z",
                       "published_date_raw": "2008-12-01T00:00:00Z",
                       "indexed_at": "2012-10-01T18:58:11.184Z",
                       "url": "https://link.springer.com/article/10.1208/s12249-008-9160-7",
                       "has_text": true,
                       "has_pdf": true
                     }""";

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Jariya Kowapradit and Praneet Opanasopit and Tanasait Ngawhiranpat")
                .withField(StandardField.TITLE, "Methylated N-(4-N,N-Dimethylaminobenzyl) Chitosan, a Novel Chitosan Derivative, Enhances Paracellular Permeability Across Intestinal Epithelial Cells (Caco-2)")
                .withField(StandardField.DATE, "2008-12-01")
                .withField(StandardField.YEAR, "2008")
                .withField(StandardField.JOURNAL, "AAPS PharmSciTech")
                .withField(StandardField.PUBLISHER, "Springer International Publishing")
                .withField(StandardField.ISSN, "1530-9932")
                .withField(StandardField.VOLUME, "9")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "1143-1152")
                .withField(StandardField.DOI, "10.1208/s12249-008-9160-7")
                .withField(StandardField.URL, "https://link.springer.com/article/10.1208/s12249-008-9160-7")
                .withField(new UnknownField("scholarapi"), "7184")
                .withField(new UnknownField("scholarApiHasText"), "true")
                .withField(new UnknownField("scholarApiHasPdf"), "true");

        assertEquals(expected, ScholarFetcher.jsonItemToBibEntry(new JSONObject(jsonString)));
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
        return List.of("unsupported");
    }

    @Override
    public String getTestJournal() {
        return "unsupported";
    }
}
