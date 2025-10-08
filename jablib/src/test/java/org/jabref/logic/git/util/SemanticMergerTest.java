package org.jabref.logic.git.util;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.merge.MergePlan;
import org.jabref.logic.git.merge.SemanticMerger;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SemanticMergerTest {
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setup() {
        SystemReader.setInstance(new NoopGitSystemReader());

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @ParameterizedTest
    @MethodSource
    void patchDatabase(String description, String base, String local, String remote, String expectedAuthor) throws Exception {
        BibDatabaseContext baseDatabaseContext = BibDatabaseContext.of(base, importFormatPreferences);
        BibDatabaseContext localDatabaseContext = BibDatabaseContext.of(local, importFormatPreferences);
        BibDatabaseContext remoteDatabaseContext = BibDatabaseContext.of(remote, importFormatPreferences);

        MergePlan plan = SemanticConflictDetector.extractMergePlan(baseDatabaseContext, localDatabaseContext, remoteDatabaseContext);
        SemanticMerger.applyMergePlan(localDatabaseContext, plan);

        BibEntry patched = localDatabaseContext.getDatabase().getEntryByCitationKey("a").orElseThrow();
        if (expectedAuthor == null) {
            assertTrue(patched.getField(StandardField.AUTHOR).isEmpty());
        } else {
            assertEquals(Optional.of(expectedAuthor), patched.getField(StandardField.AUTHOR));
        }
    }

    // These test cases are based on documented scenarios from docs/code-howtos/git.md.
    static Stream<Arguments> patchDatabase() {
        return Stream.of(
                Arguments.of("T1 - remote changed a field, local unchanged",
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {bob},
                                    doi = {ExampleDoi}
                                }
                                """,
                        "bob"
                ),
                Arguments.of("T2 - local changed a field, remote unchanged",
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {alice},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi}
                                }
                                """,
                        "alice"
                ),
                Arguments.of("T3 - both changed to same value",
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {bob},
                                    doi = {ExampleDoi}
                                }
                                """,
                        """
                                @article{a,
                                    author = {bob},
                                    doi = {ExampleDoi}
                                }
                                """,
                        "bob"
                ),
                Arguments.of("T4 - field removed in remote, unchanged in local",
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi},
                                }
                                """,
                        """
                                @article{a,
                                    author = {TestAuthor},
                                    doi = {ExampleDoi},
                                }
                                """,
                        """
                                @article{a,
                                    doi = {ExampleDoi},
                                }
                                """,
                        null
                )
        );
    }
}
