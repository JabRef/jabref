package org.jabref.logic.git.merge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.logic.git.util.NoopGitSystemReader;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        SystemReader.setInstance(new NoopGitSystemReader());

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        git = Git.init()
                 .setDirectory(tempDir.toFile())
                 .setInitialBranch("main")
                 .call();

        library = tempDir.resolve("library.bib");
    }

    @AfterEach
    void cleanup() {
        if (git != null) {
            git.close();
        }
    }

    @ParameterizedTest
    @MethodSource
    void semanticConflicts(String description, String base, String local, String remote, boolean expectConflict) throws Exception {
        RevCommit baseCommit = writeAndCommit(base, "base", alice);
        RevCommit localCommit = writeAndCommit(local, "local", alice);
        RevCommit remoteCommit = writeAndCommit(remote, "remote", bob);

        BibDatabaseContext baseDatabaseContext = parse(baseCommit);
        BibDatabaseContext localDatabaseContext = parse(localCommit);
        BibDatabaseContext remoteDatabaseContext = parse(remoteCommit);

        List<ThreeWayEntryConflict> diffs = SemanticConflictDetector.detectConflicts(baseDatabaseContext, localDatabaseContext, remoteDatabaseContext);

        if (expectConflict) {
            assertEquals(1, diffs.size(), "Expected a conflict but found none");
        } else {
            assertTrue(diffs.isEmpty(), "Expected no conflict but found some");
        }
    }

    private BibDatabaseContext parse(RevCommit commit) throws Exception {
        String content = GitFileReader.readFileFromCommit(git, commit, Path.of("library.bib")).orElse("");
        return BibDatabaseContext.of(content, importFormatPreferences);
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author, Path file, Git git) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(file.getFileName().toString()).call();
        return git.commit().setAuthor(author).setMessage(message).call();
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author) throws Exception {
        return writeAndCommit(content, message, author, library, git);
    }

    // See docs/code-howtos/git.md for testing patterns
    static Stream<Arguments> semanticConflicts() {
        return Stream.of(
                Arguments.of("E01 - entry a does not exist anywhere",
                        "",
                        "",
                        "",
                        false),

                Arguments.of("E02 - entry a added remotely only",
                        "",
                        "",
                        """
                                    @article{a,
                                        author = {remote},
                                        title = {A},
                                    }
                                """,
                        false),

                Arguments.of("E03 - entry a added locally only",
                        "",
                        """
                                    @article{a,
                                        author = {local},
                                        title = {A},
                                    }
                                """,
                        "",
                        false),

                Arguments.of("E04 - entry a added on both sides with different content",
                        "",
                        """
                                    @article{a,
                                        author = {local},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {remote},
                                        title = {A},
                                    }
                                """,
                        true),

                Arguments.of("E04a - both sides added entry a with identical content",
                        "",
                        """
                                @article{a,
                                    author = {same},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {same},
                                    title = {A},
                                }
                                """,
                        false
                ),

                Arguments.of("E04b - both added entry a but changed different fields",
                        "",
                        """
                                @article{a,
                                    author = {local},
                                }
                                """,
                        """
                                @article{a,
                                    journal = {Remote Journal},
                                }
                                """,
                        false
                ),

                Arguments.of("E04c - both added entry a with conflicting field values",
                        "",
                        """
                                @article{a,
                                    author = {local},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote},
                                    title = {A},
                                }
                                """,
                        true
                ),

                Arguments.of("E05 - entry a was deleted by both",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        "",
                        "",
                        false),

                Arguments.of("E06 - local deleted entry a, remote kept it unchanged",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        "",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        false),

                Arguments.of("E07 - local deleted entry a, remote modified it",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        "",
                        """
                                    @article{a,
                                        author = {remote},
                                        title = {A},
                                    }
                                """,
                        true),

                Arguments.of("E08 - remote deleted entry a, local kept it unchanged",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        "",
                        false),

                Arguments.of("E09 - entry a unchanged in all three",
                        """
                                @article{a,
                                    author = {base},
                                    title = {A},
                                }""",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        false),

                Arguments.of("E10a - remote modified a different field, local unchanged",
                        """
                                @article{a,
                                    author = {base},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {base},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {base},
                                    year = {2025},
                                }
                                """,
                        false
                ),

                Arguments.of("E10b - remote modified the same field, local unchanged",
                        """
                                @article{a,
                                    author = {base},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {base},
                                    title = {A},
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote},
                                    title = {A},
                                }
                                """,
                        false
                ),

                Arguments.of("E11 - remote deleted entry a, local modified it",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {local},
                                        title = {A},
                                    }
                                """,
                        "",
                        true),

                Arguments.of("E12 - local modified entry a, remote unchanged",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {local},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        false),

                Arguments.of("E13a - both modified entry a but changed different fields",
                        """
                                    @article{a,
                                        author = {base},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {local},
                                        title = {A},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                        title = {B},
                                    }
                                """,
                        false
                ),

                Arguments.of("E13b - both changed same field to same value",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {common},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {common},
                                    }
                                """,
                        false
                ),

                Arguments.of("E13c - both changed same field differently",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {local},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {remote},
                                    }
                                """,
                        true
                ),
                Arguments.of("E14a - citationKey changed in local",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{b,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        false
                ),
                Arguments.of("E14b - citationKey changed in remote",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{b,
                                        author = {base},
                                    }
                                """,
                        false
                ),
                Arguments.of("E14c - citationKey renamed differently in local and remote",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{b,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{c,
                                        author = {base},
                                    }
                                """,
                        false
                ),
                Arguments.of("E15 - both added same citationKey with different content",
                        """
                                """,
                        """
                                    @article{a,
                                        title = {local},
                                    }
                                """,
                        """
                                    @article{a,
                                        title = {remote},
                                    }
                                """,
                        true
                ),
                Arguments.of("F01 - identical field value on all sides",
                        """
                                @article{a,
                                    author = {same},
                                }
                                """,
                        """
                                @article{a,
                                    author = {same},
                                }
                                """,
                        """
                                @article{a,
                                    author = {same},
                                }
                                """,
                        false
                ),
                Arguments.of("F02 - remote changed, local same as base",
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote},
                                }
                                """,
                        false
                ),
                Arguments.of("F03 - local changed, remote same as base",
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        """
                                @article{a,
                                    author = {local},
                                }
                                """,
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        false
                ),
                Arguments.of("F04 - both changed to same value",
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        """
                                @article{a,
                                    author = {common},
                                }
                                """,
                        """
                                @article{a,
                                    author = {common},
                                }
                                """,
                        false
                ),
                Arguments.of("F05 - both changed same field differently",
                        """
                                @article{a,
                                    author = {base},
                                }
                                """,
                        """
                                @article{a,
                                    author = {local},
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote},
                                }
                                """,
                        true
                ),
                Arguments.of("F06 - Local deleted, remote unchanged",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        false
                ),
                Arguments.of("F07 - Remote deleted, local unchanged",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        false
                ),
                Arguments.of("F08 - Both deleted",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        false
                ),
                Arguments.of("F09 - Local changed, remote deleted",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                    @article{a,
                                    author = {local}
                                }
                                """,
                        """
                                @article{a,
                                }""",
                        true
                ),

                Arguments.of("F10 - Local deleted, remote changed",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        """
                                    @article{a,
                                    author = {remote}
                                }
                                """,
                        true
                ),

                Arguments.of("F11 - All missing",
                        """
                                    @article{a,
                                    }
                                """,
                        """
                                    @article{a,
                                    }
                                """,
                        """
                                    @article{a,
                                    }
                                """,
                        false
                ),

                Arguments.of("F12 - Local added",
                        """
                                   @article{a,
                                   }
                                """,
                        """
                                    @article{a,
                                    author = {local}
                                }
                                """,
                        """
                                   @article{a,
                                   }
                                """,
                        false
                ),

                Arguments.of("F13 - Remote added",
                        """
                                @article{a,
                                }
                                """,
                        """
                                @article{a,
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote}
                                }
                                """,
                        false
                ),

                Arguments.of("F14 - Both added same value",
                        """
                                @article{a,
                                }
                                """,
                        """
                                @article{a,
                                    author = {same}
                                }
                                """,
                        """
                                @article{a,
                                    author = {same}
                                }
                                """,
                        false
                ),

                Arguments.of("F15 - Both added different values",
                        """
                                @article{a,
                                }
                                """,
                        """
                                @article{a,
                                    author = {local}
                                }
                                """,
                        """
                                @article{a,
                                    author = {remote}
                                }
                                """,
                        true
                ),

                Arguments.of("F16 - Local modified, remote deleted (conflict)",
                        """
                                    @article{a,
                                    author = {base}
                                }
                                """,
                        """
                                    @article{a,
                                    author = {local}
                                }
                                """,
                        """
                                @article{a,
                                }""",
                        true
                ),

                Arguments.of("F17 - Local deleted and remote modified (conflict)",
                        """
                                    @article{a,
                                    author = {base}
                                    }
                                """,
                        """
                                    @article{a,
                                    }
                                """,
                        """
                                    @article{a,
                                    author = {remote}
                                    }
                                """,
                        true
                ),

                Arguments.of("F18 - No base, both added different",
                        """
                                @article{a,
                                }""",
                        """
                                    @article{a,
                                    author = {local}
                                    }
                                """,
                        """
                                    @article{a,
                                    author = {remote}
                                    }
                                """,
                        true
                ),
                Arguments.of("F19 - field order changed, same content",
                        """
                                    @article{a,
                                        title = {Hello},
                                        author = {Alice},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {Alice},
                                        title = {Hello},
                                    }
                                """,
                        """
                                    @article{a,
                                        title = {Hello},
                                        author = {Alice},
                                    }
                                """,
                        false
                ),
                Arguments.of("F20 - entryType changed in local",
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @book{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @article{a,
                                        author = {base},
                                    }
                                """,
                        false
                ),
                Arguments.of("F21 - entryType changed differently on both sides",
                        """

                                """,
                        """
                                    @book{a,
                                        author = {base},
                                    }
                                """,
                        """
                                    @inproceedings{a,
                                        author = {base},
                                    }
                                """,
                        true
                )
        );
    }

    @Test
    void extractMergePlanOnlyRemoteChangedEntryB() throws Exception {
        String base = """
                    @article{a,
                        author = {Test Author},
                        doi = {xya},
                    }
                    @article{b,
                        author = {Test Author},
                        doi = {xyz},
                    }
                """;
        String local = base;
        String remote = """
                    @article{b,
                        author = {author-b},
                        doi = {xyz},
                    }
                    @article{a,
                        author = {Test Author},
                        doi = {xya},
                    }
                """;

        RevCommit baseCommit = writeAndCommit(base, "base", alice);
        RevCommit localCommit = writeAndCommit(base, "local", alice);
        RevCommit remoteCommit = writeAndCommit(remote, "remote", bob);
        BibDatabaseContext baseDatabaseContext = parse(baseCommit);
        BibDatabaseContext localDatabaseContext = parse(localCommit);
        BibDatabaseContext remoteDatabaseContext = parse(remoteCommit);

        MergePlan plan = SemanticConflictDetector.extractMergePlan(baseDatabaseContext, localDatabaseContext, remoteDatabaseContext);

        assertEquals(1, plan.fieldPatches().size());
        assertTrue(plan.fieldPatches().containsKey("b"));

        Map<Field, String> patch = plan.fieldPatches().get("b");
        assertEquals("author-b", patch.get(StandardField.AUTHOR));
    }

    @Test
    void extractMergePlanRemoteAddsField() throws Exception {
        String base = """
                    @article{a,
                        author = {Test Author},
                        doi = {xya},
                    }
                """;
        String local = base;
        String remote = """
                    @article{a,
                        author = {Test Author},
                        doi = {xya},
                        year = {2025},
                    }
                """;

        RevCommit baseCommit = writeAndCommit(base, "base", alice);
        RevCommit localCommit = writeAndCommit(base, "local", alice);
        RevCommit remoteCommit = writeAndCommit(remote, "remote", bob);
        BibDatabaseContext baseDatabaseContext = parse(baseCommit);
        BibDatabaseContext localDatabaseContext = parse(localCommit);
        BibDatabaseContext remoteDatabaseContext = parse(remoteCommit);

        MergePlan plan = SemanticConflictDetector.extractMergePlan(baseDatabaseContext, localDatabaseContext, remoteDatabaseContext);

        assertEquals(1, plan.fieldPatches().size());
        Map<Field, String> patch = plan.fieldPatches().get("a");
        assertEquals("2025", patch.get(StandardField.YEAR));
    }

    @Test
    void noConflictWhenOnlyLineEndingsDiffer() throws Exception {
        String base = """
                @article{a,
                    comment = {line1\n\nline3\n\nline5},
                }
                """;
        String local = """
                @article{a,
                    comment = {line1\r\n\r\nline3\r\n\r\nline5},
                }
                """;
        String remote = """
                @article{a,
                    comment = {line1\n\nline3\n\nline5},
                }
                """;

        RevCommit baseCommit = writeAndCommit(base, "base", alice);
        RevCommit localCommit = writeAndCommit(local, "local", alice);
        RevCommit remoteCommit = writeAndCommit(remote, "remote", bob);

        BibDatabaseContext baseDatabaseContext = parse(baseCommit);
        BibDatabaseContext localDatabaseContext = parse(localCommit);
        BibDatabaseContext remoteDatabaseContext = parse(remoteCommit);

        List<ThreeWayEntryConflict> diffs = SemanticConflictDetector.detectConflicts(
                baseDatabaseContext,
                localDatabaseContext,
                remoteDatabaseContext
        );

        assertTrue(diffs.isEmpty(), "Line ending differences should not be considered conflicts");
    }
}
