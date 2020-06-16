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

        BibEntry master = new BibEntry(StandardEntryType.MastersThesis);
        master.setCiteKey("Diez:2014ppa");
        master.setField(StandardField.AUTHOR, "Diez, Tobias");
        master.setField(StandardField.TITLE, "Slice theorem for Fr\\'echet group actions and covariant symplectic field theory");
        master.setField(StandardField.SCHOOL, "Leipzig U.");
        master.setField(StandardField.YEAR, "2013");
        master.setField(StandardField.EPRINT, "1405.2249");
        master.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        master.setField(StandardField.PRIMARYCLASS, "math-ph");

        List<BibEntry> fetchedEntries = fetcher.performSearch("Fr\\'echet group actions field");

        assertEquals(Collections.singletonList(master), fetchedEntries);
    }

    @Test
    public void searchByIdentifierFindsEntry() throws Exception {
        BibEntry article = new BibEntry(StandardEntryType.Article);
        article.setCiteKey("Melnikov:1998pr");
        article.setField(StandardField.AUTHOR, "Melnikov, Kirill and Yelkhovsky, Alexander");
        article.setField(StandardField.TITLE, "Top quark production at threshold with O(alpha-s**2) accuracy");
        article.setField(StandardField.DOI, "10.1016/S0550-3213(98)00348-4");
        article.setField(StandardField.JOURNAL, "Nucl. Phys. B");
        article.setField(StandardField.PAGES, "59--72");
        article.setField(StandardField.VOLUME, "528");
        article.setField(StandardField.YEAR, "1998");
        article.setField(StandardField.EPRINT, "hep-ph/9802379");
        article.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        article.setField(new UnknownField("reportnumber"), "BUDKER-INP-1998-7, TTP-98-10");

        List<BibEntry> fetchedEntries = fetcher.performSearch("hep-ph/9802379");

        assertEquals(Collections.singletonList(article), fetchedEntries);

    }
}
