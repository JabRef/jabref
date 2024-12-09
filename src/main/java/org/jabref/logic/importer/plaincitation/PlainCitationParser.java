package org.jabref.logic.importer.plaincitation;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

public interface PlainCitationParser {
    Optional<BibEntry> parsePlainCitation(String text) throws FetcherException;
}
