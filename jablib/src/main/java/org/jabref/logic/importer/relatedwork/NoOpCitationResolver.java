package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Default no-op: tries a simple candidate scan; never creates new entries.
 */
public final class NoOpCitationResolver implements CitationResolver {

    @Override
    public Optional<BibEntry> resolveByKey(String citationKey, List<BibEntry> candidates) {
        for (BibEntry be : candidates) {
            if (be.getCitationKey().isPresent() && be.getCitationKey().get().equals(citationKey)) {
                return Optional.of(be);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<BibEntry> resolveByAuthorYear(String surnameLower, String year, List<BibEntry> candidates) {
        for (BibEntry be : candidates) {
            String author = be.getField(StandardField.AUTHOR).orElse("").toLowerCase(Locale.ROOT);
            String yr = be.getField(StandardField.YEAR).orElse("");
            if (author.contains(surnameLower) && yr.equals(year)) {
                return Optional.of(be);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<BibEntry> createIfMissing(String citationDisplay) {
        return Optional.empty();
    }
}
