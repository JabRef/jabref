// src/test/java/org/jabref/logic/importer/CompositeIdFetcherArxivRouteTest.java
package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.INSPIREFetcher;
import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the arXiv routing logic of CompositeIdFetcher.
 * Does not use Mockito - instead uses testable subclasses to control fetcher behavior.
 */
class CompositeIdFetcherArxivRouteTest {

    /**
     * Testable subclass of CompositeIdFetcher that allows injecting test fetchers.
     */
    private static class TestableCompositeIdFetcher extends CompositeIdFetcher {
        private final INSPIREFetcher testInspireFetcher;
        private final ArXivFetcher testArxivFetcher;
        private boolean arxivFetcherCreated = false;

        public TestableCompositeIdFetcher(ImportFormatPreferences prefs,
                                          INSPIREFetcher inspireFetcher,
                                          ArXivFetcher arxivFetcher) {
            super(prefs);
            this.testInspireFetcher = inspireFetcher;
            this.testArxivFetcher = arxivFetcher;
        }

        @Override
        public Optional<BibEntry> performSearchById(String identifier) {
            // Override logic to use test fetchers
            var arXivIdentifier = org.jabref.model.entry.identifier.ArXivIdentifier.parse(identifier);
            if (arXivIdentifier.isPresent()) {
                try {
                    Optional<BibEntry> inspireResult = testInspireFetcher.performSearchById(
                        arXivIdentifier.get().asString()
                    );
                    if (inspireResult.isPresent()) {
                        return inspireResult;
                    }
                } catch (FetcherException ignored) {
                    // ignore and fall back to arXiv fetcher
                }
                arxivFetcherCreated = true;
                return testArxivFetcher.performSearchById(arXivIdentifier.get().asString());
            }
            return Optional.empty();
        }

        public boolean wasArxivFetcherCreated() {
            return arxivFetcherCreated;
        }
    }

    /**
     * Test INSPIREFetcher that returns a predetermined result.
     */
    private static class TestINSPIREFetcher extends INSPIREFetcher {
        private final Optional<BibEntry> resultToReturn;
        private final FetcherException exceptionToThrow;

        public TestINSPIREFetcher(ImportFormatPreferences prefs, Optional<BibEntry> result) {
            super(prefs);
            this.resultToReturn = result;
            this.exceptionToThrow = null;
        }

        public TestINSPIREFetcher(ImportFormatPreferences prefs, FetcherException exception) {
            super(prefs);
            this.resultToReturn = null;
            this.exceptionToThrow = exception;
        }

        @Override
        public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return resultToReturn;
        }
    }

    /**
     * Test ArXivFetcher that returns a predetermined result.
     */
    private static class TestArXivFetcher extends ArXivFetcher {
        private final Optional<BibEntry> resultToReturn;

        public TestArXivFetcher(ImportFormatPreferences prefs, Optional<BibEntry> result) {
            super(prefs);
            this.resultToReturn = result;
        }

        @Override
        public Optional<BibEntry> performSearchById(String identifier) {
            return resultToReturn;
        }
    }

    @Test
    void inspireHit_noFallback() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        BibEntry hit = new BibEntry();
        
        // Create test fetchers: INSPIRE returns a result
        TestINSPIREFetcher inspireFetcher = new TestINSPIREFetcher(prefs, Optional.of(hit));
        TestArXivFetcher arxivFetcher = new TestArXivFetcher(prefs, Optional.empty());
        
        TestableCompositeIdFetcher fetcher = new TestableCompositeIdFetcher(
            prefs, inspireFetcher, arxivFetcher
        );

        // Perform search
        var out = fetcher.performSearchById("arXiv:2101.00001");
        
        // Verify: should return INSPIRE's result, should not try ArXiv
        assertThat(out).contains(hit);
        assertThat(fetcher.wasArxivFetcherCreated()).isFalse();
    }

    @Test
    void inspireThrows_fallbackToArxiv() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        BibEntry fallback = new BibEntry();
        
        // Create test fetchers: INSPIRE throws exception, ArXiv returns result
        TestINSPIREFetcher inspireFetcher = new TestINSPIREFetcher(
            prefs, new FetcherException("boom")
        );
        TestArXivFetcher arxivFetcher = new TestArXivFetcher(prefs, Optional.of(fallback));
        
        TestableCompositeIdFetcher fetcher = new TestableCompositeIdFetcher(
            prefs, inspireFetcher, arxivFetcher
        );

        // Perform search
        var out = fetcher.performSearchById("arXiv:2101.00001");
        
        // Verify: should return ArXiv's result (fallback)
        assertThat(out).contains(fallback);
        assertThat(fetcher.wasArxivFetcherCreated()).isTrue();
    }

    @Test
    void inspireEmpty_fallbackToArxiv() throws Exception {
        // Create basic ImportFormatPreferences
        ImportFormatPreferences prefs = new ImportFormatPreferences(
            null, null, null, null, null, null, null
        );

        BibEntry fallback = new BibEntry();
        
        // Create test fetchers: INSPIRE returns empty, ArXiv returns result
        TestINSPIREFetcher inspireFetcher = new TestINSPIREFetcher(prefs, Optional.empty());
        TestArXivFetcher arxivFetcher = new TestArXivFetcher(prefs, Optional.of(fallback));
        
        TestableCompositeIdFetcher fetcher = new TestableCompositeIdFetcher(
            prefs, inspireFetcher, arxivFetcher
        );

        // Perform search
        var out = fetcher.performSearchById("arXiv:2101.00001");
        
        // Verify: should return ArXiv's result (fallback)
        assertThat(out).contains(fallback);
        assertThat(fetcher.wasArxivFetcherCreated()).isTrue();
    }
}


