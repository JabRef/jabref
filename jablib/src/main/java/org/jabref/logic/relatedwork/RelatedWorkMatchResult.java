package org.jabref.logic.relatedwork;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
/// parsedReference: parsed bib entry in Reference section
/// matchedLibraryBibEntry: matched bib entry in the library
public record RelatedWorkMatchResult(
        String contextText,
        String citationKey,
        BibEntry parsedReference,
        Optional<BibEntry> matchedLibraryBibEntry
) {
    public boolean hasMatchedLibraryEntry() {
        return matchedLibraryBibEntry.isPresent();
    }
}
