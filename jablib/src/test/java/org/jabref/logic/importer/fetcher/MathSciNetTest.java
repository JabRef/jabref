package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.NormalizeWhitespacesCleanup;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class MathSciNetTest {
    private MathSciNet fetcher;
    private BibEntry ratiuEntry;
    private NormalizeWhitespacesCleanup normalizeWhitespacesCleanup;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        fetcher = new MathSciNet(importFormatPreferences);
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        normalizeWhitespacesCleanup = new NormalizeWhitespacesCleanup(fieldPreferences);

        ratiuEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("MR3537908")
                .withField(StandardField.AUTHOR, "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.")
                .withField(StandardField.TITLE, "Existence and uniqueness theorems for the two-dimensional {E}ricksen-{L}eslie system")
                .withField(StandardField.JOURNAL, "Journal of Mathematical Fluid Mechanics")
                .withField(StandardField.VOLUME, "18")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "571--589")
                .withField(StandardField.KEYWORDS, "76A15 (35A01 35A02 35K61 82D30)")
                .withField(StandardField.MR_NUMBER, "3537908")
                .withField(StandardField.ISSN, "1422-6928,1422-6952")
                .withField(StandardField.DOI, "10.1007/s00021-016-0250-0");
    }

    @Test
    void searchByEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry()
                .withField(StandardField.TITLE, "existence")
                .withField(StandardField.AUTHOR, "Ratiu")
                .withField(StandardField.JOURNAL, "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        if (!fetchedEntries.isEmpty()) {
            normalizeWhitespacesCleanup.cleanup(fetchedEntries.getFirst());
        }
        assertEquals(List.of(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response. One single call goes through, but subsequent calls fail.")
    void searchByIdInEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry()
                .withField(StandardField.MR_NUMBER, "3537908");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        if (!fetchedEntries.isEmpty()) {
            normalizeWhitespacesCleanup.cleanup(fetchedEntries.getFirst());
        }
        assertEquals(List.of(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response. One single call goes through, but subsequent calls fail.")
    void searchByQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"Existence and uniqueness theorems Two-Dimensional Ericksen Leslie System\"");
        assertFalse(fetchedEntries.isEmpty());
        BibEntry secondEntry = fetchedEntries.get(1);
        normalizeWhitespacesCleanup.cleanup(secondEntry);
        assertEquals(ratiuEntry, secondEntry);
    }

    @Test
    void getParser() throws IOException, ParseException {
        String fileName = "mathscinet.json";
        try (InputStream is = MathSciNetTest.class.getResourceAsStream(fileName)) {
            List<BibEntry> entries = fetcher.getParser().parseEntries(is);
            assertEquals(
                    new BibEntry(StandardEntryType.Article)
                            .withField(StandardField.TITLE, "On the weights of general MDS codes")
                            .withField(StandardField.AUTHOR, "Alderson, Tim L.")
                            .withField(StandardField.YEAR, "2020")
                            .withField(StandardField.JOURNAL, "IEEE Trans. Inform. Theory")
                            .withField(StandardField.VOLUME, "66")
                            .withField(StandardField.NUMBER, "9")
                            .withField(StandardField.PAGES, "5414--5418")
                            .withField(StandardField.MR_NUMBER, "4158623")
                            .withField(StandardField.KEYWORDS, "Bounds on codes")
                            .withField(StandardField.DOI, "10.1109/TIT.2020.2977319"),
                    entries.getFirst());
        }
    }
}
