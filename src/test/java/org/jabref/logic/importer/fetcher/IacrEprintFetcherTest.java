package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@FetcherTest
public class IacrEprintFetcherTest {

    private IacrEprintFetcher fetcher;
    private BibEntry abram2017;
    private BibEntry beierle2016;
    private BibEntry delgado2017;

    @BeforeEach
    public void setUp() {
        fetcher = new IacrEprintFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        abram2017 = new BibEntry();
        abram2017.setType(BiblatexEntryTypes.MISC);
        abram2017.setField("bibtexkey", "cryptoeprint:2017:1118");
        abram2017.setField(FieldName.ABSTRACT, "dummy");
        abram2017.setField(FieldName.AUTHOR, "Ittai Abraham and Dahlia Malkhi and Kartik Nayak and Ling Ren and Alexander Spiegelman");
        abram2017.setField(FieldName.DATE, "2017-11-18");
        abram2017.setField(FieldName.HOWPUBLISHED, "Cryptology ePrint Archive, Report 2017/1118");
        abram2017.setField(FieldName.NOTE, "\\url{https://eprint.iacr.org/2017/1118}");
        abram2017.setField(FieldName.TITLE, "Solida: A Blockchain Protocol Based on Reconfigurable Byzantine Consensus");
        abram2017.setField(FieldName.URL, "https://eprint.iacr.org/2017/1118/20171124:064527");
        abram2017.setField(FieldName.VERSION, "20171124:064527");
        abram2017.setField(FieldName.YEAR, "2017");

        beierle2016 = new BibEntry();
        beierle2016.setType(BiblatexEntryTypes.MISC);
        beierle2016.setField("bibtexkey", "cryptoeprint:2016:119");
        beierle2016.setField(FieldName.ABSTRACT, "dummy");
        beierle2016.setField(FieldName.AUTHOR, "Christof Beierle and Thorsten Kranz and Gregor Leander");
        beierle2016.setField(FieldName.DATE, "2017-02-17");
        beierle2016.setField(FieldName.HOWPUBLISHED, "Cryptology ePrint Archive, Report 2016/119");
        beierle2016.setField(FieldName.NOTE, "\\url{https://eprint.iacr.org/2016/119}");
        beierle2016.setField(FieldName.TITLE, "Lightweight Multiplication in GF(2^n) with Applications to MDS Matrices");
        beierle2016.setField(FieldName.URL, "https://eprint.iacr.org/2016/119/20170217:150415");
        beierle2016.setField(FieldName.VERSION, "20170217:150415");
        beierle2016.setField(FieldName.YEAR, "2016");

        delgado2017 = new BibEntry();
        delgado2017.setType(BiblatexEntryTypes.MISC);
        delgado2017.setField("bibtexkey", "cryptoeprint:2017:1095");
        delgado2017.setField(FieldName.ABSTRACT, "dummy");
        delgado2017.setField(FieldName.AUTHOR, "Sergi Delgado-Segura and Cristina Pérez-Solà and Guillermo Navarro-Arribas and Jordi Herrera-Joancomartí");
        delgado2017.setField(FieldName.DATE, "2017-11-10");
        delgado2017.setField(FieldName.HOWPUBLISHED, "Cryptology ePrint Archive, Report 2017/1095");
        delgado2017.setField(FieldName.NOTE, "\\url{https://eprint.iacr.org/2017/1095}");
        delgado2017.setField(FieldName.TITLE, "Analysis of the Bitcoin UTXO set");
        delgado2017.setField(FieldName.URL, "https://eprint.iacr.org/2017/1095/20171110:183926");
        delgado2017.setField(FieldName.VERSION, "20171110:183926");
        delgado2017.setField(FieldName.YEAR, "2017");
    }

    @Test
    public void searchByIdWithValidId1() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("Report 2017/1118 ");
        assertFalse(fetchedEntry.get().getField(FieldName.ABSTRACT).get().isEmpty());
        fetchedEntry.get().setField(FieldName.ABSTRACT, "dummy");
        assertEquals(Optional.of(abram2017), fetchedEntry);
    }

    @Test
    public void searchByIdWithValidId2() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("iacr ePrint 2016/119");
        assertFalse(fetchedEntry.get().getField(FieldName.ABSTRACT).get().isEmpty());
        fetchedEntry.get().setField(FieldName.ABSTRACT, "dummy");
        assertEquals(Optional.of(beierle2016), fetchedEntry);
    }

    @Test
    public void searchByIdWithValidIdAndNonAsciiChars() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("some random 2017/1095 stuff around the id");
        assertFalse(fetchedEntry.get().getField(FieldName.ABSTRACT).get().isEmpty());
        fetchedEntry.get().setField(FieldName.ABSTRACT, "dummy");
        assertEquals(Optional.of(delgado2017), fetchedEntry);
    }

    @Test
    public void searchByIdWithEmptyIdFails() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById(""));
    }

    @Test
    public void searchByIdWithInvalidReportNumberFails() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("2016/1"));
    }

    @Test
    public void searchByIdWithInvalidYearFails() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("16/115"));
    }

    @Test
    public void searchByIdWithInvalidIdFails() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("asdf"));
    }

    @Test
    public void searchForNonexistentIdFails() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("2016/6425"));
    }

    @Test
    public void testGetName() {
        assertEquals(IacrEprintFetcher.NAME, fetcher.getName());
    }
}
