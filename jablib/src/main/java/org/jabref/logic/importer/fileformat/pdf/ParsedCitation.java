
package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// Structured metadata extracted from a book citation.
@NullMarked
record ParsedCitation(
        Optional<String> title,
        Optional<String> authors,
        Optional<String> editors,
        Optional<String> publisher,
        Optional<String> year,
        Optional<String> edition
) {
}
