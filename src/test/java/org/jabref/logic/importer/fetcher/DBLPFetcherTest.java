package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class DBLPFetcherTest {

    private DBLPFetcher dblpFetcher;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences())
                .thenReturn(mock(FieldContentParserPreferences.class));
        dblpFetcher = new DBLPFetcher(importFormatPreferences);
        entry = new BibEntry();

        entry.setType(BibtexEntryTypes.ARTICLE.getName());
        entry.setCiteKey("DBLP:journals/stt/GeigerHL16");
        entry.setField(FieldName.TITLE,
                "Process Engine Benchmarking with Betsy in the Context of {ISO/IEC} Quality Standards");
        entry.setField(FieldName.AUTHOR, "Matthias Geiger and Simon Harrer and J{\\\"{o}}rg Lenhard");
        entry.setField(FieldName.JOURNAL, "Softwaretechnik-Trends");
        entry.setField(FieldName.VOLUME, "36");
        entry.setField(FieldName.NUMBER, "2");
        entry.setField(FieldName.YEAR, "2016");
        entry.setField(FieldName.URL,
                "http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf");
        entry.setField("biburl", "https://dblp.org/rec/bib/journals/stt/GeigerHL16");
        entry.setField("bibsource", "dblp computer science bibliography, https://dblp.org");

    }

    @Test
    public void findSingleEntry() throws FetcherException {
        String query = "Process Engine Benchmarking with Betsy in the Context of {ISO/IEC} Quality Standards";
        List<BibEntry> result = dblpFetcher.performSearch(query);

        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void findSingleEntryUsingComplexOperators() throws FetcherException {
        String query = "geiger harrer betsy$ softw.trends"; //-wirtz Negative operators do no longer work,  see issue https://github.com/JabRef/jabref/issues/2890
        List<BibEntry> result = dblpFetcher.performSearch(query);

        assertEquals(Collections.singletonList(entry), result);
    }

}
