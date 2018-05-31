package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class IEEETest {

    private IEEE finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        finder = new IEEE();
        entry = new BibEntry();
    }

    @Test
    void findByDOI() throws IOException {
        entry.setField("doi", "10.1109/ACCESS.2016.2535486");

        assertEquals(
                Optional.of(
                        new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    void findByDocumentUrl() throws IOException {
        entry.setField("url", "https://ieeexplore.ieee.org/document/7421926/");
        assertEquals(
                Optional.of(
                        new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    void findByURL() throws IOException {
        entry.setField("url", "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7421926");

        assertEquals(
                Optional.of(
                        new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    void findByOldURL() throws IOException {
        entry.setField("url", "https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7421926");

        assertEquals(
                Optional.of(
                        new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    void findByDOIButNotURL() throws IOException {
        entry.setField("doi", "10.1109/ACCESS.2016.2535486");
        entry.setField("url", "http://dx.doi.org/10.1109/ACCESS.2016.2535486");

        assertEquals(
                Optional.of(
                        new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void notFoundByURL() throws IOException {
        entry.setField("url", "http://dx.doi.org/10.1109/ACCESS.2016.2535486");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
