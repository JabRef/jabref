package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
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
class INSPIREFetcherTest {

    private INSPIREFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(mock(FieldContentFormatterPreferences.class));
        fetcher = new INSPIREFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.MastersThesis)
                .withCitationKey("Diez:2013fdp")
                .withField(StandardField.AUTHOR, "Diez, Tobias")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.SCHOOL, "Leipzig U.")
                .withField(StandardField.YEAR, "2013")
                .withField(StandardField.EPRINT, "1405.2249")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv")
                .withField(StandardField.PRIMARYCLASS, "math-ph");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Fréchet group actions field");
        assertEquals(Collections.singletonList(master), fetchedEntries);
    }

    @Test
    public void searchByIdentifierFindsEntry() throws Exception {
        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Melnikov:1998pr")
                .withField(StandardField.AUTHOR, "Melnikov, Kirill and Yelkhovsky, Alexander")
                .withField(StandardField.TITLE, "Top quark production at threshold with O(alpha-s**2) accuracy")
                .withField(StandardField.DOI, "10.1016/S0550-3213(98)00348-4")
                .withField(StandardField.JOURNAL, "Nucl. Phys. B")
                .withField(StandardField.PAGES, "59--72")
                .withField(StandardField.VOLUME, "528")
                .withField(StandardField.YEAR, "1998")
                .withField(StandardField.EPRINT, "hep-ph/9802379")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv")
                .withField(new UnknownField("reportnumber"), "BUDKER-INP-1998-7, TTP-98-10");
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"hep-ph/9802379\"");
        assertEquals(Collections.singletonList(article), fetchedEntries);
    }
}
