package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class Text2BibFetcherTest {

    private Text2BibFetcher fetcher;

    @BeforeEach
    public void setUp() {
        fetcher = new Text2BibFetcher(mock(ImportFormatPreferences.class));
    }

    @Test
    public void returnsSomething() throws Exception {
        String entryText = "Kopp, O.; Martin, D.; Wutke, D. & Leymann, F. The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages Enterprise Modelling and Information Systems, Gesellschaft für Informatik e.V. (GI), 2009, 4, 3-13";
        List<BibEntry> bibEntries = fetcher.performSearch(entryText);
        assertEquals(1, bibEntries.size());
        //assertEquals(bibEntry.getField(StandardField.AUTHOR), Optional.of("O Kopp and D Martin and D Wutke and F Leymann"));
    }

    @Test
    public void testGetFormatName() {
        assertEquals("text2bib", fetcher.getName());
    }
}
