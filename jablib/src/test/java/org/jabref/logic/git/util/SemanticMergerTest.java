package org.jabref.logic.git.util;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SemanticMergerTest {
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setup() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePatchCases")
    void testPatchEntry(String description, String base, String local, String remote, String expectedAuthor) throws Exception {
        BibEntry baseEntry = parseSingleEntry(base);
        BibEntry localEntry = parseSingleEntry(local);
        BibEntry remoteEntry = parseSingleEntry(remote);

        SemanticMerger.patchEntryNonConflictingFields(baseEntry, localEntry, remoteEntry);

        assertEquals(expectedAuthor, localEntry.getField(StandardField.AUTHOR).orElse(null));
    }

    @ParameterizedTest(name = "Database patch: {0}")
    @MethodSource("provideDatabasePatchCases")
    void testPatchDatabase(String description, String base, String local, String remote, String expectedAuthor) throws Exception {
        BibDatabaseContext baseCtx = GitBibParser.parseBibFromGit(base, importFormatPreferences);
        BibDatabaseContext localCtx = GitBibParser.parseBibFromGit(local, importFormatPreferences);
        BibDatabaseContext remoteCtx = GitBibParser.parseBibFromGit(remote, importFormatPreferences);

        SemanticMerger.applyRemotePatchToDatabase(baseCtx, localCtx, remoteCtx);

        BibEntry patched = localCtx.getDatabase().getEntryByCitationKey("a").orElseThrow();
        assertEquals(expectedAuthor, patched.getField(StandardField.AUTHOR).orElse(null));
    }

    static Stream<Arguments> providePatchCases() {
        return Stream.of(
                Arguments.of("Remote changed, local unchanged",
                        "@article{a, author = {X} }",
                        "@article{a, author = {X} }",
                        "@article{a, author = {Bob} }",
                        "Bob"
                ),
                Arguments.of("Local changed, remote unchanged",
                        "@article{a, author = {X} }",
                        "@article{a, author = {Alice} }",
                        "@article{a, author = {X} }",
                        "Alice"
                ),
                Arguments.of("Both changed to same value",
                        "@article{a, author = {X} }",
                        "@article{a, author = {Y} }",
                        "@article{a, author = {Y} }",
                        "Y"
                )
        );
    }

    static Stream<Arguments> provideDatabasePatchCases() {
        return Stream.of(
                // TODO: more test case
                Arguments.of("T1 - remote changed a field, local unchanged",
                        """
                        @article{a,
                            author = {lala},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {lala},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {bob},
                            doi = {xya},
                        }
                        """,
                        "bob"
                ),

                Arguments.of("T2 - local changed a field, remote unchanged",
                        """
                        @article{a,
                            author = {lala},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {alice},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {lala},
                            doi = {xya},
                        }
                        """,
                        "alice"
                ),

                Arguments.of("T3 - both changed to same value",
                        """
                        @article{a,
                            author = {lala},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {bob},
                            doi = {xya},
                        }
                        """,
                        """
                        @article{a,
                            author = {bob},
                            doi = {xya},
                        }
                        """,
                        "bob"
                )
        );
    }

    private BibEntry parseSingleEntry(String content) throws Exception {
        BibDatabaseContext context = GitBibParser.parseBibFromGit(content, importFormatPreferences);
        List<BibEntry> entries = context.getDatabase().getEntries();
        if (entries.size() != 1) {
            throw new IllegalStateException("Test assumes exactly one entry");
        }
        return entries.get(0);
    }
}
