package org.jabref.logic.importer.fileformat.pdf;

import java.util.function.UnaryOperator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;

/// Entry point for field extraction from an already located Ukrainian DSTU-style citation block.
///
/// Supports the implemented citation subset, rather than arbitrary book citation styles.
/// [BookCitationBlock] handles location and publication areas; [BookTitleAuthorExtractor] handles titles
/// and contributors. The combined metadata is returned as a [BibEntry].
@NullMarked
final class BookCitationParser {

    private BookCitationParser() {
    }

    /// Parses a citation block into its metadata fields.
    ///
    /// @param citationBlock  text returned by [BookCitationBlock#find]
    /// @param normalizeNames converts extracted contributor names into a BibTeX name list
    /// @return book entry containing the extracted metadata
    static BibEntry parse(String citationBlock, UnaryOperator<String> normalizeNames) {
        BibEntry bookEntry = new BibEntry(StandardEntryType.Book);
        BookTitleAuthorExtractor.findTitle(citationBlock).ifPresent(title -> bookEntry.withField(StandardField.TITLE, title));
        BookTitleAuthorExtractor.findAuthors(citationBlock)
                                .map(normalizeNames)
                                .ifPresent(authors -> bookEntry.withField(StandardField.AUTHOR, authors));
        BookTitleAuthorExtractor.findEditors(citationBlock)
                                .map(normalizeNames)
                                .ifPresent(editors -> bookEntry.withField(StandardField.EDITOR, editors));
        BookCitationBlock.findPublisher(citationBlock).ifPresent(publisher -> bookEntry.withField(StandardField.PUBLISHER, publisher));
        BookCitationBlock.findYear(citationBlock).ifPresent(year -> bookEntry.withField(StandardField.YEAR, year));
        BookCitationBlock.findEdition(citationBlock).ifPresent(edition -> bookEntry.withField(StandardField.EDITION, edition));
        return bookEntry;
    }
}
