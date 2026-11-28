package org.jabref.logic.importer.plaincitation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Prefers the metadata registered for a DOI over what a plain-citation parser guesses from the text.
///
/// A single citation containing a DOI is resolved via the DOI without running the parser.
/// For multiple citations, the parser runs first and every resulting entry carrying a DOI (in the DOI or URL field) is replaced by the fetched entry.
/// If the lookup fails or finds nothing, the parsed entry is kept.
///
/// [impl->req~import.plain-citation.doi-first~2]
@NullMarked
public class DoiFirstPlainCitationParser implements PlainCitationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoiFirstPlainCitationParser.class);

    private final PlainCitationParser delegate;
    private final IdBasedFetcher doiFetcher;

    public DoiFirstPlainCitationParser(PlainCitationParser delegate, IdBasedFetcher doiFetcher) {
        this.delegate = delegate;
        this.doiFetcher = doiFetcher;
    }

    @Override
    public Optional<BibEntry> parsePlainCitation(String text) throws FetcherException {
        Optional<BibEntry> fetched = DOI.findInText(text).flatMap(this::fetch);
        if (fetched.isPresent()) {
            return fetched;
        }
        return delegate.parsePlainCitation(text);
    }

    @Override
    public List<BibEntry> parseMultiplePlainCitations(String text) throws FetcherException {
        String trimmed = text.trim();
        if (!trimmed.isEmpty() && trimmed.chars().noneMatch(Character::isWhitespace)) {
            Optional<BibEntry> fetched = DOI.parse(trimmed).flatMap(this::fetch);
            if (fetched.isPresent()) {
                return List.of(fetched.orElseThrow());
            }
        }
        List<BibEntry> result = new ArrayList<>();
        for (BibEntry parsed : delegate.parseMultiplePlainCitations(text)) {
            result.add(parsed.getField(StandardField.DOI)
                             .flatMap(DOI::parse)
                             // Some parsers keep a doi.org link in the URL field only
                             .or(() -> parsed.getField(StandardField.URL).flatMap(DOI::findInText))
                             .flatMap(this::fetch)
                             .map(fetched -> retainParsedInformation(fetched, parsed))
                             .orElse(parsed));
        }
        return result;
    }

    private Optional<BibEntry> fetch(DOI doi) {
        try {
            return doiFetcher.performSearchById(doi.asString())
                             .map(entry -> {
                                 if (!entry.hasField(StandardField.DOI)) {
                                     entry.setField(StandardField.DOI, doi.asString());
                                 }
                                 return entry;
                             });
        } catch (FetcherException e) {
            LOGGER.info("Could not fetch entry for DOI {}, falling back to plain-citation parsing", doi.asString(), e);
            return Optional.empty();
        }
    }

    private BibEntry retainParsedInformation(BibEntry fetched, BibEntry parsed) {
        if (fetched.getCitationKey().isEmpty()) {
            parsed.getCitationKey().ifPresent(fetched::setCitationKey);
        }
        parsed.getFields().stream()
              .filter(field -> !fetched.hasField(field))
              .forEach(field -> parsed.getField(field).ifPresent(value -> fetched.setField(field, value)));
        return fetched;
    }
}
