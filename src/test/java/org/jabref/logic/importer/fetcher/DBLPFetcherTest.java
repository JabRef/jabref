package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
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
        when(importFormatPreferences.getFieldContentFormatterPreferences())
                .thenReturn(mock(FieldContentFormatterPreferences.class));
        dblpFetcher = new DBLPFetcher(importFormatPreferences);
        entry = new BibEntry();

        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("DBLP:journals/stt/GeigerHL16");
        entry.setField(StandardField.TITLE,
                "Process Engine Benchmarking with Betsy in the Context of {ISO/IEC} Quality Standards");
        entry.setField(StandardField.AUTHOR, "Matthias Geiger and Simon Harrer and J{\\\"{o}}rg Lenhard");
        entry.setField(StandardField.JOURNAL, "Softwaretechnik-Trends");
        entry.setField(StandardField.VOLUME, "36");
        entry.setField(StandardField.NUMBER, "2");
        entry.setField(StandardField.YEAR, "2016");
        entry.setField(StandardField.URL,
                "http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf");
        entry.setField(new UnknownField("biburl"), "https://dblp.org/rec/journals/stt/GeigerHL16.bib");
        entry.setField(new UnknownField("bibsource"), "dblp computer science bibliography, https://dblp.org");
    }

    @Test
    public void findSingleEntry() throws FetcherException {
        // In Lucene curly brackets are used for range queries, therefore they have to be escaped using "". See https://lucene.apache.org/core/5_4_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html
        String query = "Process Engine Benchmarking with Betsy in the Context of \"{ISO/IEC}\" Quality Standards";
        List<BibEntry> result = dblpFetcher.performSearch(query);

        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void findSingleEntryUsingComplexOperators() throws FetcherException {
        String query = "geiger harrer betsy$ softw.trends"; // -wirtz Negative operators do no longer work,  see issue https://github.com/JabRef/jabref/issues/2890
        List<BibEntry> result = dblpFetcher.performSearch(query);

        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void findNothing() throws Exception {
        assertEquals(Collections.emptyList(), dblpFetcher.performSearch(""));
    }
}
