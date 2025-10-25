// src/test/java/org/jabref/logic/importer/fetcher/INSPIREFetcher_TexkeysTest.java
package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests INSPIREFetcher's handling of texkeys.
 * Does not use Mockito - instead uses testable implementations.
 */
class INSPIREFetcherTexkeysTest {

    /**
     * Test Parser that returns pre-parsed BibEntry (containing texkeys field).
     */
    private static class TestParserWithTexkeys implements Parser {
        private final ImportFormatPreferences prefs;
        private final String bibtexContent;

        public TestParserWithTexkeys(ImportFormatPreferences prefs, String bibtexContent) {
            this.prefs = prefs;
            this.bibtexContent = bibtexContent;
        }

        @Override
        public List<BibEntry> parseEntries(InputStream inputStream) {
            // Ignore input stream, parse predetermined BibTeX content
            BibtexParser realParser = new BibtexParser(prefs);
            return realParser.parseEntries(new ByteArrayInputStream(bibtexContent.getBytes()));
        }
    }

    /**
     * Test URLDownload replacement.
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
    void texkeysAppliedAndCleared() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        // Prepare BibTeX content containing texkeys
        String bib = """
                @article{dummy,
                  title={T},
                  texkeys={Smith2020,Another}
                }
                """;
        
        // Create test parser that returns parsed BibEntry (containing texkeys)
        Parser testParser = new TestParserWithTexkeys(prefs, bib);
        
        TestableINSPIREFetcher fetcher = new TestableINSPIREFetcher(prefs, testParser);

        // Create an entry with DOI to trigger search
        var base = new BibEntry();
        base.setField(StandardField.DOI, "10.1000/xyz");

        // Perform search
        var out = fetcher.performSearch(base);
        
        // Verify results
        assertThat(out).hasSize(1);
        var e = out.get(0);
        
        // texkeys should be applied as citation key (first one)
        assertThat(e.getCitationKey()).hasValue("Smith2020");
        
        // texkeys field should be cleared (not shown in final result)
        assertThat(e.getField(new UnknownField("texkeys"))).isEmpty();
    }
}

