package org.jabref.logic.search;

import java.util.List;
import java.util.stream.Stream;

import javafx.util.Pair;

import org.jabref.logic.search.inmemory.MatchInformation;
import org.jabref.logic.search.inmemory.MatchInformation.PartialResult;
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

    public static final BibEntry ARTICLE_HARRER = new BibEntry(StandardEntryType.Article)
            .withCitationKey("harrer")
            .withField(StandardField.AUTHOR, "harrer");

    public static final BibEntry INCOLLECTION_TONHO = new BibEntry(StandardEntryType.InCollection)
            .withCitationKey("tonho")
            .withField(StandardField.AUTHOR, "tonho");

    public static final BibEntry EMPTY_ENTRY = new BibEntry();

    public static final BibEntry TITLE_SENTENCE_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-sentence-cased")
            .withField(StandardField.TITLE, "Title Sentence Cased")
            .withField(StandardField.AUTHOR, TITLE_SENTENCE_CASED_AUTHOR);

    public static final BibEntry TITLE_MIXED_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-mixed-cased")
            .withField(StandardField.TITLE, "TiTle MiXed CaSed")
            .withField(StandardField.AUTHOR, TITLE_MIXED_CASED_AUTHOR);

    public static final BibEntry TITLE_UPPER_CASED = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("title-upper-cased")
            .withField(StandardField.TITLE, "TITLE UPPER CASED")
            .withField(StandardField.AUTHOR, TITLE_UPPER_CASED_AUTHOR);

    public static final BibEntry AUTHOR_WITH_COMMENT_CHARACTERS = new BibEntry(StandardEntryType.Misc)
            .withField(StandardField.TITLE, "A life with a stupid name - my story")
            .withField(StandardField.AUTHOR, AUTHOR_WITH_COMMENT_CHARACTERS_NAME);

    private static final String TITLE_SENTENCE_CASED_AUTHOR = "JamesSensible";
    private static final String TITLE_MIXED_CASED_AUTHOR = "TomTheHusky";
    private static final String TITLE_UPPER_CASED_AUTHOR = "ArnoldtheLoud";
    private static final String AUTHOR_WITH_COMMENT_CHARACTERS_NAME = "Commen/*tiusPavloczyns*/ky";

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
                        new SearchQuery("any =~ [Z]"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED))
        );
    }

    public static Stream<Arguments> detailedSearchCases() {
        return Stream.of(
                // empty library
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of()),
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of(EMPTY_ENTRY)),
                Arguments.of(List.of(), new SearchQuery("whatever"), List.of(EMPTY_ENTRY, ARTICLE_HARRER, INCOLLECTION_TONHO)),

                // invalid search syntax → no matches
                Arguments.of(List.of(), new SearchQuery("author="), List.of(ARTICLE_HARRER)),

                // unfielded bareword (case-insensitive substring on any field);
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                ARTICLE_HARRER,
                                new MatchInformation(true, new PartialResult(true, "harrer")))
                        ),
                        new SearchQuery("harrer"), List.of(ARTICLE_HARRER)
                ),

                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                INCOLLECTION_TONHO,
                                new MatchInformation(true, new PartialResult(true, "tonho")))
                        ),
                        new SearchQuery("tonho"), List.of(INCOLLECTION_TONHO)
                ),

                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                INCOLLECTION_TONHO,
                                new MatchInformation(true, new PartialResult(true, "tonho")))
                        ),
                        new SearchQuery("tonho"), List.of(ARTICLE_HARRER, INCOLLECTION_TONHO)
                ),

                // fielded queries
                Arguments.of(List.of(), new SearchQuery("title= harrer"), List.of(ARTICLE_HARRER)),
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                ARTICLE_HARRER,
                                new MatchInformation(true, new PartialResult(true, "author=harrer")))
                        ),
                        new SearchQuery("author=harrer"), List.of(ARTICLE_HARRER)
                ),

                // case-insensitive vs case-sensitive contains (=, =!)
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                        TITLE_SENTENCE_CASED,
                                        new MatchInformation(true, new PartialResult(true, "title=Title"))),
                                new Pair<BibEntry, MatchInformation>(
                                        TITLE_MIXED_CASED,
                                        new MatchInformation(true, new PartialResult(true, "title=Title"))),
                                new Pair<BibEntry, MatchInformation>(
                                        TITLE_UPPER_CASED,
                                        new MatchInformation(true, new PartialResult(true, "title=Title")))

                        ),
                        new SearchQuery("title=Title"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),

                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                TITLE_SENTENCE_CASED,
                                new MatchInformation(true, new PartialResult(true, "title=!Title")))
                        ),
                        new SearchQuery("title=!Title"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),

                Arguments.of(List.of(), new SearchQuery("title =! TiTLE"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),

                // any-field with case-sensitive contains (any =!)
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                TITLE_MIXED_CASED,
                                new MatchInformation(true, new PartialResult(true, "any=!TiTle")))
                        ),
                        new SearchQuery("any=!TiTle"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),
                Arguments.of(List.of(), new SearchQuery("any=!TiTLe"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),

                // regex on any field
                Arguments.of(List.of(), new SearchQuery("any =~ [Z]"), List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)),

                // testing partial results
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                        TITLE_SENTENCE_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(false, "any=~husky"),
                                                new PartialResult(true, "any=~james")
                                        )
                                ),
                                new Pair<BibEntry, MatchInformation>(
                                        TITLE_MIXED_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(true, "any=~husky"),
                                                new PartialResult(false, "any=~james")
                                        )
                                )
                        ),
                        new SearchQuery("any=~husky or any=~james"),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),

                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                        TITLE_UPPER_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(true, "title=title"),
                                                new PartialResult(true, String.format("author=%s", TITLE_UPPER_CASED_AUTHOR)),
                                                new PartialResult(false, String.format("author=%s", TITLE_MIXED_CASED_AUTHOR))
                                        )
                                ),
                                new Pair<BibEntry, MatchInformation>(
                                        TITLE_MIXED_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(true, "title=title"),
                                                new PartialResult(false, String.format("author=%s", TITLE_UPPER_CASED_AUTHOR)),
                                                new PartialResult(true, String.format("author=%s", TITLE_MIXED_CASED_AUTHOR))
                                        )
                                )
                        ),
                        new SearchQuery(String.format("title=title AND (author=%s OR author=%s)", TITLE_UPPER_CASED_AUTHOR, TITLE_MIXED_CASED_AUTHOR)),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),

                // comments do not modify the query
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                        TITLE_UPPER_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(true, "title=title"),
                                                new PartialResult(true, String.format("author=%s", TITLE_UPPER_CASED_AUTHOR)),
                                                new PartialResult(false, String.format("author=%s", TITLE_MIXED_CASED_AUTHOR))
                                        )
                                ),
                                new Pair<BibEntry, MatchInformation>(
                                        TITLE_MIXED_CASED,
                                        new MatchInformation(true,
                                                new PartialResult(true, "title=title"),
                                                new PartialResult(false, String.format("author=%s", TITLE_UPPER_CASED_AUTHOR)),
                                                new PartialResult(true, String.format("author=%s", TITLE_MIXED_CASED_AUTHOR))
                                        )
                                )
                        ),
                        new SearchQuery(String.format("title=title /*Old Tom Bombadil is a merry fellow*/ AND (author=%s /*Bright blue his jacket is*/ OR author=%s)", TITLE_UPPER_CASED_AUTHOR, TITLE_MIXED_CASED_AUTHOR)),
                        List.of(TITLE_SENTENCE_CASED, TITLE_MIXED_CASED, TITLE_UPPER_CASED)
                ),

                // comment delimiters inside a string literal are treated as a part of this string literal
                Arguments.of(List.of(new Pair<BibEntry, MatchInformation>(
                                AUTHOR_WITH_COMMENT_CHARACTERS,
                                new MatchInformation(true, new PartialResult(true, String.format("author=%s", AUTHOR_WITH_COMMENT_CHARACTERS_NAME))))
                        ),
                        new SearchQuery(String.format("author=%s", AUTHOR_WITH_COMMENT_CHARACTERS_NAME)), List.of(AUTHOR_WITH_COMMENT_CHARACTERS)
                )

        );
    }
}
