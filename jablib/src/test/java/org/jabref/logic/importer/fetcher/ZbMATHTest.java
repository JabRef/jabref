package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.core.GetRequest;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ZbMATHTest {
    private ZbMATH fetcher;
    private BibEntry donaldsonEntry;
    private URL bibtexFileUrl;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        fetcher = Mockito.spy(new ZbMATH(importFormatPreferences));

        donaldsonEntry = new BibEntry();
        donaldsonEntry.setType(StandardEntryType.Article);
        donaldsonEntry.setCitationKey("zbMATH03800580");
        donaldsonEntry.setField(StandardField.AUTHOR, "Donaldson, S. K.");
        donaldsonEntry.setField(StandardField.JOURNAL, "Journal of Differential Geometry");
        donaldsonEntry.setField(StandardField.DOI, "10.4310/jdg/1214437665");
        donaldsonEntry.setField(StandardField.ISSN, "0022-040X");
        donaldsonEntry.setField(StandardField.LANGUAGE, "English");
        donaldsonEntry.setField(StandardField.KEYWORDS, "57N13,57R10,53C05,58J99,57R65");
        donaldsonEntry.setField(StandardField.PAGES, "279--315");
        donaldsonEntry.setField(StandardField.TITLE, "An application of gauge theory to four dimensional topology");
        donaldsonEntry.setField(StandardField.VOLUME, "18");
        donaldsonEntry.setField(StandardField.YEAR, "1983");
        donaldsonEntry.setField(StandardField.ZBL_NUMBER, "0507.57010");
        donaldsonEntry.setField(new UnknownField("zbmath"), "3800580");

        Path bibtexFile = tempDir.resolve("zbmath_bibtex.bib");
        Files.writeString(bibtexFile, """
                @Article{zbMATH03800580,
                  author     = {Donaldson, S. K.},
                  journal    = {Journal of Differential Geometry},
                  title      = {An application of gauge theory to four dimensional topology},
                  year       = {1983},
                  issn       = {0022-040X},
                  pages      = {279--315},
                  volume     = {18},
                  doi        = {10.4310/jdg/1214437665},
                  language   = {English},
                  keywords   = {57N13,57R10,53C05,58J99,57R65},
                  zbl        = {0507.57010},
                  zbmath     = {3800580}
                }
                """);
        bibtexFileUrl = bibtexFile.toUri().toURL();

        Mockito.doReturn(bibtexFileUrl).when(fetcher).getUrlForIdentifier(anyString());
        Mockito.doReturn(bibtexFileUrl).when(fetcher).getURLForQuery(any());
    }

    private void mockUnirest(boolean isEmptySearch, Runnable testRunnable) {
        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            GetRequest getRequest = mock(GetRequest.class);
            unirestMock.when(() -> Unirest.get(anyString())).thenReturn(getRequest);

            @SuppressWarnings("unchecked")
            HttpResponse<JsonNode> httpResponse = mock(HttpResponse.class);
            when(getRequest.asJson()).thenReturn(httpResponse);
            when(httpResponse.getStatus()).thenReturn(200);

            JsonNode jsonNode = mock(JsonNode.class);
            when(httpResponse.getBody()).thenReturn(jsonNode);

            JSONObject jsonObject = new JSONObject();
            JSONArray resultsArray = new JSONArray();
            if (!isEmptySearch) {
                JSONObject matchObj = new JSONObject();
                matchObj.put("zbl_id", "0507.57010");
                resultsArray.put(matchObj);
            }
            jsonObject.put("results", resultsArray);
            when(jsonNode.getObject()).thenReturn(jsonObject);

            testRunnable.run();
        }
    }

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByIdFindsEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0507.57010");
        assertEquals(Optional.of(donaldsonEntry), fetchedEntry);
    }

    @Test
    void searchByEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "An application of gauge theory to four dimensional topology");
        searchEntry.setField(StandardField.AUTHOR, "S. K. {Donaldson}");

        mockUnirest(false, () -> {
            try {
                List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
                assertEquals(List.of(donaldsonEntry), fetchedEntries);
            } catch (FetcherException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void searchByNoneEntryFindsNothing() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "t");
        searchEntry.setField(StandardField.AUTHOR, "a");

        mockUnirest(true, () -> {
            try {
                List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
                assertEquals(List.of(), fetchedEntries);
            } catch (FetcherException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void searchByIdInEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.ZBL_NUMBER, "0507.57010");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }
}
