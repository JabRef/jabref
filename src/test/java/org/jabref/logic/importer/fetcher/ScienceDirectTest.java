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
class ScienceDirectTest {

    private ScienceDirect finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new ScienceDirect();
        entry = new BibEntry();
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiOldPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.jrmge.2015.08.004");

        assertEquals(
                Optional.of(new URL("https://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiNewPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.09.002");

        assertEquals(
                Optional.of(new URL("https://www.sciencedirect.com/science/article/pii/S2212671614001024/pdf?md5=4e2e9a369b4d5b3db5100aba599bef8b&pid=1-s2.0-S2212671614001024-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiWorksForBoneArticle() throws IOException {
        // The DOI is an example by a user taken from https://github.com/JabRef/jabref/issues/5860
        entry.setField(StandardField.DOI, "https://doi.org/10.1016/j.bone.2020.115226");

        assertEquals(
                Optional.of(new URL("https://www.sciencedirect.com/science/article/pii/S8756328220300065/pdfft?md5=0ad75ff155637dec358e5c9fb8b90afd&pid=1-s2.0-S8756328220300065-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notFoundByDoi() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
