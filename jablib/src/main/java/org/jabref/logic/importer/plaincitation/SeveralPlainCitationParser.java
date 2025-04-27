package org.jabref.logic.importer.plaincitation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.UncheckedException;

public class SeveralPlainCitationParser {
    private final PlainCitationParser parser;

    public SeveralPlainCitationParser(PlainCitationParser parser) {
        this.parser = parser;
    }

    public List<BibEntry> parseSeveralPlainCitations(String text) throws FetcherException {
        try {
            return splitCitations(text)
                    .map(Unchecked.function(parser::parsePlainCitation))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (UncheckedException e) {
            throw (FetcherException) e.getCause();
        }
    }

    public Stream<String> splitCitations(String text) {
        return Arrays.stream(text.split("\\r\\r+|\\n\\n+|\\r\\n(\\r\\n)+"))
                     .map(String::trim)
                     .filter(str -> !str.isBlank());
    }
}
