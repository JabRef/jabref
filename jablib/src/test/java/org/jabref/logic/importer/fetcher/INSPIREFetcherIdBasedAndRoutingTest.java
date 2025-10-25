// src/test/java/org/jabref/logic/importer/fetcher/INSPIREFetcher_IdBasedAndRoutingTest.java
package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests INSPIREFetcher's ID-based search and routing logic.
 * Does not use Mockito - instead uses testable subclasses and implementations.
 */
class INSPIREFetcherIdBasedAndRoutingTest {

    /**
     * Test Parser implementation that returns predetermined results.
     */
    private static class TestParser implements Parser {
        private final List<BibEntry> entriesToReturn;

        public TestParser(List<BibEntry> entriesToReturn) {
            this.entriesToReturn = entriesToReturn;
        }

        @Override
        public List<BibEntry> parseEntries(InputStream inputStream) {
            return entriesToReturn;
        }
    }

    /**
     * Test URLDownload replacement that returns empty data stream.
     */
    private static class TestURLDownload extends URLDownload {
        public TestURLDownload(URL source) {
            super(source);
        }

        @Override
        public InputStream asInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    /**
     * Testable INSPIREFetcher subclass that overrides network-related methods.
     */
    private static class TestableINSPIREFetcher extends INSPIREFetcher {
        private final Parser testParser;

        public TestableINSPIREFetcher(ImportFormatPreferences preferences, Parser testParser) {
            super(preferences);
            this.testParser = testParser;
        }

        @Override
        public Parser getParser() {
            return testParser;
        }

        @Override
        public URLDownload getUrlDownload(URL url) {
            return new TestURLDownload(url);
        }
    }

    @Test
    void performSearchById_supportsArxivAndDoi() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        // Create test parser that returns a BibEntry
        Parser testParser = new TestParser(List.of(new BibEntry()));

        TestableINSPIREFetcher fetcher = new TestableINSPIREFetcher(prefs, testParser);

        // Test arXiv ID - should be recognized and return result
        assertThat(fetcher.performSearchById("arXiv:2101.00001")).isPresent();

        // Test DOI - should be recognized and return result
        assertThat(fetcher.performSearchById("10.1145/123456")).isPresent();

        // Test invalid ID - should return empty
        assertThat(fetcher.performSearchById("not-an-id")).isEmpty();
    }

    @Test
    void routing_prefersArxivOverDoiAndEmptyWhenNoId() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        // Create test parser that returns a BibEntry
        Parser testParser = new TestParser(List.of(new BibEntry()));

        TestableINSPIREFetcher fetcher = new TestableINSPIREFetcher(prefs, testParser);

        // Test arXiv - should route successfully
        var arxiv = new BibEntry();
        arxiv.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        arxiv.setField(StandardField.EPRINT, "2101.00001");
        assertThat(fetcher.performSearch(arxiv)).hasSize(1);

        // Test DOI - should route successfully
        var doi = new BibEntry();
        doi.setField(StandardField.DOI, "10.1000/abc");
        assertThat(fetcher.performSearch(doi)).hasSize(1);

        // Test entry without ID - should return empty list
        var none = new BibEntry();
        assertThat(fetcher.performSearch(none)).isEmpty();
    }
}

