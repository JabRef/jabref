package org.jabref.logic.search;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.params.provider.Arguments;

/// Shared search test cases that every [LibrarySearcher] implementation must satisfy.
///
/// Cases here must be implementation-agnostic: they may not depend on Lucene-indexed
/// fulltext or Postgres-specific behavior. Implementation-specific cases (e.g., FULLTEXT)
/// belong in the per-implementation test class.
public final class LibrarySearcherTestCases {

    public static final BibEntry EMPTY_ENTRY = new BibEntry();

    public static final BibEntry ARTICLE_HARRER = new BibEntry(StandardEntryType.Article)
            .withCitationKey("harrer")
            .withField(StandardField.AUTHOR, "harrer");

    public static final BibEntry INCOLLECTION_TONHO = new BibEntry(StandardEntryType.InCollection)
            .withCitationKey("tonho")
            .withField(StandardField.AUTHOR, "tonho");

    public static final BibEntry TITLE_SENTENCE_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-sentence-cased")
            .withField(StandardField.TITLE, "Title Sentence Cased");

    public static final BibEntry TITLE_MIXED_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-mixed-cased")
            .withField(StandardField.TITLE, "TiTle MiXed CaSed");

    public static final BibEntry TITLE_UPPER_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-upper-cased")
            .withField(StandardField.TITLE, "TITLE UPPER CASED");

    private LibrarySearcherTestCases() {
    }

    /// Cases that any [LibrarySearcher] implementation must pass.
    /// Argument tuple: `(expectedMatches, query, libraryEntries)`.
    public static Stream<Arguments> commonSearchCases() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of()),
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of(EMPTY_ENTRY)),
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of(EMPTY_ENTRY, ARTICLE_HARRER, INCOLLECTION_TONHO)),

                // invalid search syntax → no matches
                Arguments.of(List.of(), new SearchQuery("author="), List.of(ARTICLE_HARRER)),

                // unfielded bareword (case-insensitive substring on any field)
                Arguments.of(List.of(ARTICLE_HARRER), new SearchQuery("harrer"), List.of(ARTICLE_HARRER)),
                Arguments.of(List.of(INCOLLECTION_TONHO), new SearchQuery("tonho"), List.of(INCOLLECTION_TONHO)),
                Arguments.of(List.of(INCOLLECTION_TONHO), new SearchQuery("tonho"), List.of(ARTICLE_HARRER, INCOLLECTION_TONHO)),

                // fielded queries
                Arguments.of(List.of(), new SearchQuery("title= harrer"), List.of(ARTICLE_HARRER)),
                Arguments.of(List.of(ARTICLE_HARRER), new SearchQuery("author= harrer"), List.of(ARTICLE_HARRER)),

                // case-insensitive vs case-sensitive contains (=, =!)
                Arguments.of(List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED),
                        new SearchQuery("title = Title"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),
                Arguments.of(List.of(TITLE_SENTENCE_CASED),
                        new SearchQuery("title =! Title"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),
                Arguments.of(List.of(),
                        new SearchQuery("title =! TiTLE"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),

                // any-field with case-sensitive contains (any =!)
                Arguments.of(List.of(TITLE_MIXED_CASED),
                        new SearchQuery("any =! TiTle"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),
                Arguments.of(List.of(),
                        new SearchQuery("any =! TiTLE"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),

                // regex on any field
                Arguments.of(List.of(),
                        new SearchQuery("any =~ [Y]"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED))
        );
    }
}
