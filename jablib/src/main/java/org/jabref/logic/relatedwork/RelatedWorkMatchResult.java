package org.jabref.logic.relatedwork;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RelatedWorkMatchResult(
        String contextText,
        String citationKey,
        // parsed bib entry in Reference section
        Optional<BibEntry> parsedReference,
        // matched bib entry in the library
        Optional<BibEntry> matchedLibraryBibEntry
) {
    public boolean hasMatchedLibraryEntry() {
        return matchedLibraryBibEntry.isPresent();
    }
}
