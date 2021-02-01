package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class TitleFetcherTest {

    private TitleFetcher fetcher;
    private BibEntry bibEntryBischof2009;

    @BeforeEach
    public void setUp() {
        fetcher = new TitleFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntryBischof2009 = new BibEntry();
        bibEntryBischof2009.setType(StandardEntryType.InProceedings);
        bibEntryBischof2009.setCitationKey("Bischof_2009");
        bibEntryBischof2009.setField(StandardField.AUTHOR, "Marc Bischof and Oliver Kopp and Tammo van Lessen and Frank Leymann");
        bibEntryBischof2009.setField(StandardField.BOOKTITLE, "2009 35th Euromicro Conference on Software Engineering and Advanced Applications");
        bibEntryBischof2009.setField(StandardField.PUBLISHER, "{IEEE}");
        bibEntryBischof2009.setField(StandardField.TITLE, "{BPELscript}: A Simplified Script Syntax for {WS}-{BPEL} 2.0");
        bibEntryBischof2009.setField(StandardField.YEAR, "2009");
        bibEntryBischof2009.setField(StandardField.MONTH, "aug");
        bibEntryBischof2009.setField(StandardField.DOI, "10.1109/seaa.2009.21");
    }

    @Test
    public void testGetName() {
        assertEquals("Title", fetcher.getName());
    }

    @Test
    public void testPerformSearchKopp2007() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("BPELscript: A simplified script syntax for WS-BPEL 2.0");
        assertEquals(Optional.of(bibEntryBischof2009), fetchedEntry);
    }

    @Test
    public void testPerformSearchEmptyTitle() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchInvalidTitle() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("An unknown title where noi DOI can be determined");
        assertEquals(Optional.empty(), fetchedEntry);
    }
}
