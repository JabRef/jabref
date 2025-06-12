package org.jabref.logic.git.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticConflictDetectorTest {
    private Git git;
    private Path library;
    private final PersonIdent alice = new PersonIdent("Alice", "alice@example.org");
    private final PersonIdent bob = new PersonIdent("Bob", "bob@example.org");

    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        git = Git.init()
                 .setDirectory(tempDir.toFile())
                 .setInitialBranch("main")
                 .call();

        library = tempDir.resolve("library.bib");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideConflictCases")
    void testSemanticConflicts(String description, String base, String local, String remote, boolean expectConflict) throws Exception {
        RevCommit baseCommit = writeAndCommit(base, "base", alice);
        RevCommit localCommit = writeAndCommit(local, "local", alice);
        RevCommit remoteCommit = writeAndCommit(remote, "remote", bob);

        BibDatabaseContext baseCtx = parse(baseCommit);
        BibDatabaseContext localCtx = parse(localCommit);
        BibDatabaseContext remoteCtx = parse(remoteCommit);

        List<BibEntryDiff> diffs = SemanticConflictDetector.detectConflicts(baseCtx, localCtx, remoteCtx);

        if (expectConflict) {
            assertEquals(1, diffs.size(), "Expected a conflict but found none");
        } else {
            assertTrue(diffs.isEmpty(), "Expected no conflict but found some");
        }
    }

    private BibDatabaseContext parse(RevCommit commit) throws Exception {
        String content = GitFileReader.readFileFromCommit(git, commit, Path.of("library.bib"));
        return GitBibParser.parseBibFromGit(content, importFormatPreferences);
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author, Path file, Git git) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(file.getFileName().toString()).call();
        return git.commit().setAuthor(author).setMessage(message).call();
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author) throws Exception {
        return writeAndCommit(content, message, author, library, git);
    }

    static Stream<Arguments> provideConflictCases() {
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
                        false
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
                        false
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
                        false
                ),
                Arguments.of("T4 - both changed to different values",
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
                                author = {bob},
                                doi = {xya},
                            }
                        """,
                        true
                ),
                Arguments.of("T5 - local deleted field, remote changed it",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        """
                            @article{a,
                            }
                        """,
                        """
                            @article{a,
                                author = {bob},
                                doi = {xya},
                            }
                        """,
                        true
                ),
                Arguments.of("T6 - local changed, remote deleted",
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
                            }
                        """,
                        true
                ),
                Arguments.of("T7 - remote deleted, local unchanged",
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
                            }
                        """,
                        false
                ),
                Arguments.of("T8 - local changed field A, remote changed field B",
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
                                doi = {xyz},
                            }
                        """,
                        false
                ),
                Arguments.of("T9 - field order changed only",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        """
                            @article{a,
                                doi = {xya},
                                author = {lala},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        false
                ),
                Arguments.of("T10 - local changed entry a, remote changed entry b",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                              }

                            @article{b,
                                author = {lala},
                                doi = {xyz},
                            }
                        """,
                        """
                            @article{a,
                                author = {author-a},
                                doi = {xya},
                            }
                            @article{b,
                                author = {lala},
                                doi = {xyz},
                            }
                        """,
                        """
                            @article{b,
                                author = {author-b},
                                doi = {xyz},
                            }

                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        false
                ),
                Arguments.of("T11 - remote added field, local unchanged",
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
                                author = {lala},
                                doi = {xya},
                                year = {2025},
                            }
                        """,
                        false
                ),
                Arguments.of("T12 - both added same field with different values",
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
                                year = {2023},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                                year = {2025},
                            }
                        """,
                        true
                ),
                Arguments.of("T13 - local added field, remote unchanged",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {newfield},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        false
                ),
                Arguments.of("T14 - both added same field with same value",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {value},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {value},
                            }
                        """,
                        false
                ),
                Arguments.of("T15 - both added same field with different values",
                        """
                            @article{a,
                                author = {lala},
                                doi = {xya},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {value1},
                            }
                        """,
                        """
                            @article{a,
                                author = {lala},
                                doi = {value2},
                            }
                        """,
                        true
                )
        );
    }
}
