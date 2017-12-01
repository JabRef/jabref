package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@Category(FetcherTest.class)
public class IacrEprintFetcherTest {

    private final static int TIMEOUT_FOR_TESTS = 5000;

    private IacrEprintFetcher fetcher;
    private BibEntry abram2017;
    private BibEntry beierle2016;

    @Before
    public void setUp() {
        fetcher = new IacrEprintFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        abram2017 = new BibEntry();
        abram2017.setType(BiblatexEntryTypes.MISC);
        abram2017.setField("bibtexkey", "cryptoeprint:2017:1118");
        abram2017.setField(FieldName.ABSTRACT, "The decentralized cryptocurrency Bitcoin has experienced great success but also encountered many challenges. One of the challenges has been the long confirmation time. Another challenge is the lack of incentives at certain steps of the protocol, raising concerns for transaction withholding, selfish mining, etc. To address these challenges, we propose Solida, a decentralized blockchain protocol based on reconfigurable Byzantine consensus augmented by proof-of-work. Solida improves on Bitcoin in confirmation time,  and provides safety  and liveness assuming the adversary control less than (roughly) one-third of the total mining power.\n");
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
        beierle2016.setField(FieldName.ABSTRACT, "In this paper we consider the fundamental question of optimizing finite field multiplications with one fixed element. Surprisingly, this question did not receive much attention previously. We investigate which field representation, that is which choice of basis, allows for an optimal implementation. Here, the efficiency of the multiplication is measured in terms of the number of XOR operations needed to implement the multiplication. While our results are potentially of larger interest, we focus on a particular application in the second part of our paper. Here we construct new MDS matrices which outperform or are on par with all previous results when focusing on a round-based hardware implementation.\n");
        beierle2016.setField(FieldName.AUTHOR, "Christof Beierle and Thorsten Kranz and Gregor Leander");
        beierle2016.setField(FieldName.DATE, "2017-02-17");
        beierle2016.setField(FieldName.HOWPUBLISHED, "Cryptology ePrint Archive, Report 2016/119");
        beierle2016.setField(FieldName.NOTE, "\\url{https://eprint.iacr.org/2016/119}");
        beierle2016.setField(FieldName.TITLE, "Lightweight Multiplication in GF(2^n) with Applications to MDS Matrices");
        beierle2016.setField(FieldName.URL, "https://eprint.iacr.org/2016/119/20170217:150415");
        beierle2016.setField(FieldName.VERSION, "20170217:150415");
        beierle2016.setField(FieldName.YEAR, "2016");
    }

    @Test(timeout = TIMEOUT_FOR_TESTS)
    public void searchByIdWithValidId1() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("Report 2017/1118 ");
        assertEquals(Optional.of(abram2017), fetchedEntry);
    }

    @Test(timeout = TIMEOUT_FOR_TESTS)
    public void searchByIdWithValidId2() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("iacr ePrint 2016/119");
        assertEquals(Optional.of(beierle2016), fetchedEntry);
    }

    @Test(timeout = TIMEOUT_FOR_TESTS, expected = FetcherException.class)
    public void searchByIdWithEmptyIdFails() throws FetcherException {
        fetcher.performSearchById("");
    }

    @Test(timeout = TIMEOUT_FOR_TESTS, expected = FetcherException.class)
    public void searchByIdWithInvalidReportNumberFails() throws FetcherException {
        fetcher.performSearchById("2016/1");
    }

    @Test(timeout = TIMEOUT_FOR_TESTS, expected = FetcherException.class)
    public void searchByIdWithInvalidYearFails() throws FetcherException {
        fetcher.performSearchById("16/115");
    }

    @Test(timeout = TIMEOUT_FOR_TESTS, expected = FetcherException.class)
    public void searchByIdWithInvalidIdFails() throws FetcherException {
        fetcher.performSearchById("asdf");
    }

    @Test(timeout = TIMEOUT_FOR_TESTS, expected = FetcherException.class)
    public void searchForNonexistentIdFails() throws FetcherException {
        fetcher.performSearchById("2016/6425");
    }

    @Test
    public void testGetName() {
        assertEquals(IacrEprintFetcher.NAME, fetcher.getName());
    }
}
