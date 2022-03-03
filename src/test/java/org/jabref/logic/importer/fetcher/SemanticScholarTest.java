package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class SemanticScholarTest {


    private static final String URL_PDF = "http://dl.ifip.org/db/conf/networking/networking2021/1570714032.pdf";
    private static final String DOI = "10.23919/IFIPNetworking52078.2021.9472772";

    private static final String URL_PDF2 = "https://europepmc.org/articles/pmc4907333?pdf=render";
    private static final String DOI2 = "10.1038/nrn3241";

    private static final String URL_PDF3 = "https://pdfs.semanticscholar.org/7f6e/61c254bc2df38a784c1228f56c13317caded.pdf";
    private static final String DOI3 = "10.3390/healthcare9020206";

    private static final String URL_PDF4 = "https://arxiv.org/pdf/1407.3561.pdf";
    private static final String ARXIV = "1407.3561";

    private SemanticScholar finder;
    private BibEntry entry;
    private BibEntry entry2;
    private BibEntry entry3;
    private BibEntry entry4;

    @BeforeEach
    void setUp() {
        finder = new SemanticScholar();
        entry = new BibEntry().withField(StandardField.DOI, DOI);
        entry2 = new BibEntry().withField(StandardField.DOI, DOI2);
        entry3 = new BibEntry().withField(StandardField.DOI, DOI3);
        entry4 = new BibEntry().withField(StandardField.EPRINT, ARXIV)
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv");
    }

    @Test
    void getDocument() {
        String source = null;
        try {
            source = finder.getURLBySource(
                    String.format("https://api.semanticscholar.org/v1/paper/%s", entry.getDOI().get().getDOI()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals("https://www.semanticscholar.org/paper/7f7b38604a2c167f6d5fb1c5dffcbb127d0525c0", source);
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByDOI() throws IOException {
        assertEquals(
                Optional.of(new URL(URL_PDF2)),
                finder.findFullText(entry2)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByDOIAlternate() throws IOException {
        assertEquals(
                Optional.of(new URL(URL_PDF3)),
                finder.findFullText(entry3)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextSearchOnEmptyEntry() throws IOException {

        assertEquals(Optional.empty(), finder.findFullText(new BibEntry()));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByArXiv() throws IOException {
        assertEquals(
                Optional.of(new URL(URL_PDF4)),
                finder.findFullText(entry4)
        );
    }

    @Test
    void fullTextEntityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(new BibEntry()));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.PUBLISHER, finder.getTrustLevel());
    }
}
