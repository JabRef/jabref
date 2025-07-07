package org.jabref.logic.git.util;

import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.io.GitBibParser;
import org.jabref.logic.git.merge.MergePlan;
import org.jabref.logic.git.merge.SemanticMerger;
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

    @ParameterizedTest(name = "Database patch: {0}")
    @MethodSource("provideDatabasePatchCases")
    void patchDatabase(String description, String base, String local, String remote, String expectedAuthor) throws Exception {
        BibDatabaseContext baseDatabaseContext = GitBibParser.parseBibFromGit(base, importFormatPreferences);
        BibDatabaseContext localDatabaseContext = GitBibParser.parseBibFromGit(local, importFormatPreferences);
        BibDatabaseContext remoteDatabaseContext = GitBibParser.parseBibFromGit(remote, importFormatPreferences);

        MergePlan plan = SemanticConflictDetector.extractMergePlan(baseDatabaseContext, remoteDatabaseContext);
        SemanticMerger.applyMergePlan(localDatabaseContext, plan);

        BibEntry patched = localDatabaseContext.getDatabase().getEntryByCitationKey("a").orElseThrow();
        assertEquals(expectedAuthor, patched.getField(StandardField.AUTHOR).orElse(null));
    }

    static Stream<Arguments> provideDatabasePatchCases() {
        return Stream.of(
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
}
