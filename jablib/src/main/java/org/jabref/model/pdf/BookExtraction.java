package org.jabref.model.pdf;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISBN;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record BookExtraction(Optional<BibEntry> localBook, List<ISBN> isbnCandidates) {
    public BookExtraction {
        isbnCandidates = List.copyOf(isbnCandidates);
    }
}
