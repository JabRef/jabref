package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class ISIDOREFetcherTest {

    private ISIDOREFetcher fetcher;

    @BeforeEach
    public void setup() {
        this.fetcher = new ISIDOREFetcher();
    }

    @Test
    public void checkArticle() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Investigating day-to-day variability of transit usage on a multimonth scale with smart card data. A case study in Lyon")
                .withField(StandardField.AUTHOR, "Oscar Egu and Patrick Bonnel")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Travel Behaviour and Society")
                .withField(StandardField.PUBLISHER, "HAL CCSD, Elsevier")
                .withField(StandardField.DOI, "10.1016/j.tbs.2019.12.003")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.hrzlqd");

        Optional<BibEntry> actual = fetcher.performSearchById("https://isidore.science/document/10670/1.hrzlqd");

        assertEquals(Optional.of(expected), actual);
    }

    @Test
    public void checkArticle2() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "“ Anthony B. Atkinson, Inequality – What Can Be Done ? Cambridge (Mass.) Harvard University Press, 2015, XI-384 p. ”")
                .withField(StandardField.AUTHOR, "Benoît Rapoport")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.JOURNAL, "Population (édition française)")
                .withField(StandardField.PUBLISHER, "HAL CCSD, INED - Institut national d’études démographiques")
                .withField(StandardField.DOI, "10.3917/popu.1601.0153")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.d2vlam");

        Optional<BibEntry> actual = fetcher.performSearchById("d2vlam");

        assertEquals(Optional.of(expected), actual);
    }

    @Test
    public void checkThesis() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Thesis)
                .withField(StandardField.TITLE, "Mapping English L2 errors : an integrated system and textual approach")
                .withField(StandardField.AUTHOR, "Clive Hamilton")
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.m05oth");

        Optional<BibEntry> actual = fetcher.performSearchById("1.m05oth");

        assertEquals(Optional.of(expected), actual);
    }

    @Test
    public void checkArticle3() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Salvage Lymph Node Dissection for Nodal Recurrent Prostate Cancer: A Systematic Review.")
                .withField(StandardField.AUTHOR, "G. Ploussard and G. Gandaglia and H. Borgmann and P. de Visschere and I. Heidegger and A. Kretschmer and R. Mathieu and C. Surcel and D. Tilki and I. Tsaur and M. Valerio and R. van den Bergh and P. Ost and A. Briganti")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.JOURNAL, "European urology")
                .withField(StandardField.DOI, "10.1016/j.eururo.2018.10.041")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.zm7q2x");

        Optional<BibEntry> actual = fetcher.performSearchById("https://isidore.science/document/10670/1.zm7q2x");

        assertEquals(Optional.of(expected), actual);
    }
}
