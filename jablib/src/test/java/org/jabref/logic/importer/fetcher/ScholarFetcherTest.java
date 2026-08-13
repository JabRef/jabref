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
                {\r
                       "id": "7184",\r
                       "title": "Methylated N-(4-N,N-Dimethylaminobenzyl) Chitosan, a Novel Chitosan Derivative, Enhances Paracellular Permeability Across Intestinal Epithelial Cells (Caco-2)",\r
                       "authors": [
                         "Jariya Kowapradit",
                         "Praneet Opanasopit",
                         "Tanasait Ngawhiranpat"
                       ],\r
                       "abstract": "The aim of this study was to investigate the effect of methylated N-(4-N,N-dimethylaminobenzyl) chitosan, TM-Bz-CS, on the paracellular permeability of Caco-2 cell monolayers and its toxicity towards the cell lines. The factors affecting epithelial permeability, e.g., degree of quaternization (DQ) and extent of dimethylaminobenzyl substitution (ES), were evaluated in intestinal cell monolayers of Caco-2 cells using the transepithelial electrical resistance and permeability of Caco-2 cell monolayers, with fluorescein isothiocyanate dextran 4,400 (FD-4) as a model compound for paracellular tight-junction transport. Cytotoxicity was evaluated with the 3-(4,5-dimethylthiazol-2-yl)-2,5-diphenyl tetrazolium bromide viability assay. The results revealed that, at pH 7.4, TM-Bz-CS appeared to increase cell permeability in a concentration-dependent manner, and this effect was relatively reversible at lower doses of 0.05–0.5 mM. Higher DQ and the ES caused the permeability of FD-4 to be higher. The cytotoxicity of TM-Bz-CS depended on concentration, %DQ, and %ES. These studies demonstrated that this novel modified chitosan has potential as an absorption enhancer.",\r
                       "journal": "AAPS PharmSciTech",\r
                       "journal_publisher": "Springer International Publishing",\r
                       "journal_issn": [
                         "1530-9932"
                       ],\r
                       "journal_issue": "Volume 9, Issue 4",\r
                       "journal_pages": "1143-1152",\r
                       "doi": "10.1208/s12249-008-9160-7"\r,
                       "published_date": "2008-12-01T00:00:00Z"\r,
                       "published_date_raw": "2008-12-01T00:00:00Z",\r
                       "indexed_at": "2012-10-01T18:58:11.184Z",\r
                       "url": "https://link.springer.com/article/10.1208/s12249-008-9160-7",\r
                       "has_text": true,\r
                       "has_pdf": true\r
                     }""";

        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = ScholarFetcher.jsonItemToBibEntry(jsonObject);

        assertEquals(Optional.of("7184"), bibEntry.getField(new UnknownField("scholarapi")));
        assertEquals(Optional.of("2008-12-01"), bibEntry.getField(StandardField.DATE));
        assertEquals(Optional.of("2008"), bibEntry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Methylated N-(4-N,N-Dimethylaminobenzyl) Chitosan, a Novel Chitosan Derivative, Enhances Paracellular Permeability Across Intestinal Epithelial Cells (Caco-2)"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Jariya Kowapradit and Praneet Opanasopit and Tanasait Ngawhiranpat"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("AAPS PharmSciTech"), bibEntry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("1530-9932"), bibEntry.getField(StandardField.ISSN));
        assertEquals(Optional.of("9"), bibEntry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("4"), bibEntry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("1143-1152"), bibEntry.getField(StandardField.PAGES));
        assertEquals(Optional.of("10.1208/s12249-008-9160-7"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("https://link.springer.com/article/10.1208/s12249-008-9160-7"), bibEntry.getField(StandardField.URL));
        assertEquals(Optional.of("Springer International Publishing"), bibEntry.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("true"), bibEntry.getField(new UnknownField("scholarApiHasText")));
        assertEquals(Optional.of("true"), bibEntry.getField(new UnknownField("scholarApiHasPdf")));
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
