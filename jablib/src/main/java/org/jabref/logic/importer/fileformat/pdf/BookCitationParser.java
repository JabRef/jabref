
package org.jabref.logic.importer.fileformat.pdf;

import org.jspecify.annotations.NullMarked;

/// Combines book citation metadata into a structured result.
@NullMarked
final class BookCitationParser {

    private BookCitationParser() {
    }

    /// Parses a citation block into its metadata fields.
    ///
    /// @param citationBlock text returned by BookCitationBlock.find
    /// @return structured citation metadata
    static ParsedCitation parse(String citationBlock) {
        return new ParsedCitation(
                BookTitleAuthorExtractor.findTitle(citationBlock),
                BookTitleAuthorExtractor.findAuthors(citationBlock),
                BookTitleAuthorExtractor.findEditors(citationBlock),
                BookCitationBlock.findPublisher(citationBlock),
                BookCitationBlock.findYear(citationBlock),
                BookCitationBlock.findEdition(citationBlock)
        );
    }
}
