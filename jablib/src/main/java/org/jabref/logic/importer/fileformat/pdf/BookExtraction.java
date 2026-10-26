package org.jabref.logic.importer.fileformat.pdf;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISBN;

import org.jspecify.annotations.NullMarked;

@NullMarked
record BookExtraction(Optional<BibEntry> localBook, List<ISBN> isbnCandidates) {
    BookExtraction {
        isbnCandidates = isbnCandidates.stream()
                                       .map(ISBN::asIsbn13)
                                       .flatMap(Optional::stream)
                                       .distinct()
                                       .toList();
    }
}
