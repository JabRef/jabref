package org.jabref.logic.git.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticConflictDetectorTest {
    private Git git;
    private Path library;
    private RevCommit baseCommit;
    private RevCommit localCommit;
    private RevCommit remoteCommitNoConflict;
    private RevCommit remoteCommitConflict;

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

        String base = """
                    @article{a,
                       author = {lala},
                       doi = {xya},
                     }

                     @article{b,
                       author = {author-b},
                       doi = {xyz},
                     }
                """;

        String local = """
                    @article{a,
                       author = {author-a},
                       doi = {xya},
                     }

                     @article{b,
                       author = {author-b},
                       doi = {xyz},
                     }
                """;

        String remoteNoConflict = """
                    @article{b,
                       author = {author-b},
                       doi = {xyz},
                     }

                    @article{a,
                       author = {lala},
                       doi = {xya},
                     }
                """;

        String remoteConflict = """
            @article{b,
                       author = {author-b},
                       doi = {xyz},
                     }

                    @article{a,
                       author = {author-c},
                       doi = {xya},
                     }
            """;

        baseCommit = writeAndCommit(base, "base", alice, library, git);
        localCommit = writeAndCommit(local, "local change article a - author a", alice, library, git);

        // Remote with no conflict
        git.checkout().setStartPoint(baseCommit).setCreateBranch(true).setName("remote-noconflict").call();
        remoteCommitNoConflict = writeAndCommit(remoteNoConflict, "remote change article b", bob, library, git);

        // Remote with conflict
        git.checkout().setStartPoint(baseCommit).setCreateBranch(true).setName("remote-conflict").call();
        remoteCommitConflict = writeAndCommit(remoteConflict, "remote change article a - author c", bob, library, git);
    }

    @Test
    void detectsNoConflictWhenChangesAreInDifferentFields() throws Exception {
        BibDatabaseContext base = parse(baseCommit);
        BibDatabaseContext local = parse(localCommit);
        BibDatabaseContext remote = parse(remoteCommitNoConflict);

        List<BibEntryDiff> diffs = SemanticConflictDetector.detectConflicts(base, local, remote);
        assertTrue(diffs.isEmpty(), "Expected no semantic conflict, but found some");
    }

    @Test
    void detectsConflictWhenSameFieldModifiedDifferently() throws Exception {
        BibDatabaseContext base = parse(baseCommit);
        BibDatabaseContext local = parse(localCommit);
        BibDatabaseContext remote = parse(remoteCommitConflict);

        List<BibEntryDiff> diffs = SemanticConflictDetector.detectConflicts(base, local, remote);
        assertEquals(1, diffs.size(), "Expected one conflicting entry");

        BibEntryDiff diff = diffs.get(0);
        BibEntry localEntry = diff.originalEntry(); // from local
        BibEntry remoteEntry = diff.newEntry();     // from remote

        String localAuthor = localEntry.getField(StandardField.AUTHOR).orElse("");
        String remoteAuthor = remoteEntry.getField(StandardField.AUTHOR).orElse("");

        assertEquals("author-a", localAuthor);
        assertEquals("author-c", remoteAuthor);
        assertTrue(!localAuthor.equals(remoteAuthor), "Expected AUTHOR field conflict in entry 'a'");
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

    private BibEntry findEntryByCitationKey(BibDatabaseContext ctx, String key) {
        return ctx.getDatabase().getEntries().stream()
                  .filter(entry -> entry.getCitationKey().orElse("").equals(key))
                  .findFirst()
                  .orElseThrow(() -> new IllegalStateException("Entry with key '" + key + "' not found"));
    }
}
