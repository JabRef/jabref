package org.jabref.logic.importer.fetcher;

import java.util.Arrays;
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
        BibEntry phd = new BibEntry(StandardEntryType.PhdThesis);
        phd.setCiteKey("Diez:2019pkg");
        phd.setField(StandardField.AUTHOR, "Diez, Tobias");
        phd.setField(StandardField.TITLE, "Normal Form of Equivariant Maps and Singular Symplectic Reduction in Infinite Dimensions with Applications to Gauge Field Theory");
        phd.setField(StandardField.YEAR, "2019");
        phd.setField(StandardField.EPRINT, "1909.00744");
        phd.setField(new UnknownField("reportnumber"), "urn:nbn:de:bsz:15-qucosa2-352179");
        phd.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        phd.setField(StandardField.PRIMARYCLASS, "math.SG");

        BibEntry article = new BibEntry(StandardEntryType.Article);
        article.setCiteKey("Diez:2018gjz");
        article.setField(StandardField.AUTHOR, "Diez, Tobias and Rudolph, Gerd");
        article.setField(StandardField.TITLE, "Singular symplectic cotangent bundle reduction of gauge field theory");
        article.setField(StandardField.YEAR, "2018");
        article.setField(StandardField.EPRINT, "1812.04707");
        article.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        article.setField(StandardField.PRIMARYCLASS, "math-ph");

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

        assertEquals(Arrays.asList(phd, article, master), fetchedEntries);
    }
}
