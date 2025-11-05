package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.UncheckedException;

public interface PlainCitationParser {
    Optional<BibEntry> parsePlainCitation(String text) throws FetcherException;

    /// Note that this is similar to `importDatabase`
    default List<BibEntry> parseMultiplePlainCitations(String text) throws FetcherException {
        try {
            return CitationSplitter.splitCitations(text)
                    .map(Unchecked.function(this::parsePlainCitation))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (UncheckedException e) {
            throw (FetcherException) e.getCause();
        }
    }
}
