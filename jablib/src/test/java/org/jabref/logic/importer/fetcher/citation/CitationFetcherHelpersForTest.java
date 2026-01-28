package org.jabref.logic.importer.fetcher.citation;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

public class CitationFetcherHelpersForTest {
    public static class Mocks {
        public static CitationFetcher from(
                Function<BibEntry, List<BibEntry>> retrieveCitedBy,
                Function<BibEntry, List<BibEntry>> retrieveCiting,
                Function<BibEntry, Optional<Integer>> retrieveCitationCount
        ) {
            return new CitationFetcher() {
                @Override
                public List<BibEntry> getCitations(BibEntry entry) {
                    return retrieveCitedBy.apply(entry);
                }

                @Override
                public List<BibEntry> getReferences(BibEntry entry) {
                    return retrieveCiting.apply(entry);
                }

                @Override
                public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
                    return retrieveCitationCount.apply(entry);
                }

                @Override
                public String getName() {
                    return "Test citation fetcher";
                }
            };
        }
    }
}
