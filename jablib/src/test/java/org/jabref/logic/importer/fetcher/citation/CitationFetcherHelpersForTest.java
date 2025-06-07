package org.jabref.logic.importer.fetcher.citation;

import java.util.List;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

public class CitationFetcherHelpersForTest {
    public static class Mocks {
        public static CitationFetcher from(
            Function<BibEntry, List<BibEntry>> retrieveCitedBy,
            Function<BibEntry, List<BibEntry>> retrieveCiting
        ) {
            return new CitationFetcher() {
                @Override
                public List<BibEntry> searchCitedBy(BibEntry entry) {
                    return retrieveCitedBy.apply(entry);
                }

                @Override
                public List<BibEntry> searchCiting(BibEntry entry) {
                    return retrieveCiting.apply(entry);
                }

                @Override
                public String getName() {
                    return "Test citation fetcher";
                }
            };
        }
    }
}
