package org.jabref.model.pdf;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISBN;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record BookMetadataExtractionResult(Optional<BibEntry> localBook, List<ISBN> isbnCandidates) {
    public BookMetadataExtractionResult {
        isbnCandidates = List.copyOf(isbnCandidates);
    }
}
