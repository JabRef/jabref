package net.sf.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DBLPFetcherTest {

    private DBLPFetcher dblpFetcher;
    private BibEntry entry;

    @Before
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
        entry.setField("biburl", "http://dblp.dagstuhl.de/rec/bib/journals/stt/GeigerHL16");
        entry.setField("bibsource", "dblp computer science bibliography, http://dblp.org");

    }

    @Test
    public void findSingleEntry() throws FetcherException {
        String query = "Process Engine Benchmarking with Betsy in the Context of {ISO/IEC} Quality Standards";
        List<BibEntry> result = dblpFetcher.performSearch(query);

        Assert.assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void findSingleEntryUsingComplexOperators() throws FetcherException {
        String query = "geiger harrer -wirtz betsy$ softw.trends";
        List<BibEntry> result = dblpFetcher.performSearch(query);

        Assert.assertEquals(Collections.singletonList(entry), result);
    }

}
