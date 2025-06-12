package org.jabref.logic.git;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.git.util.GitFileReader;
import org.jabref.logic.importer.ImportFormatPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitSyncServiceTest {
    private Git git;
    private Path library;
    private ImportFormatPreferences importFormatPreferences;

    // These are setup by alieBobSetting
    private RevCommit baseCommit;
    private RevCommit aliceCommit;
    private RevCommit bobCommit;

    private final PersonIdent alice = new PersonIdent("Alice", "alice@example.org");
    private final PersonIdent bob = new PersonIdent("Bob", "bob@example.org");
    private final String initialContent = """
            @article{a,
              author = {don't know the author}
              doi = {xya},
            }

            @article{b,
              author = {author-b}
              doi = {xyz},
            }
        """;

    // Alice modifies a
    private final String aliceUpdatedContent = """
            @article{a,
              author = {author-a}
              doi = {xya},
            }

            @article{b,
              author = {author-b}
              doi = {xyz},
            }
        """;

    // Bob reorders a and b
    private final String bobUpdatedContent = """
            @article{b,
              author = {author-b}
              doi = {xyz},
            }

            @article{a,
              author = {lala}
              doi = {xya},
            }
            """;


    /**
     * Creates a commit graph with a base commit, one modification by Alice and one modification by Bob
     */
    @BeforeEach
    void aliceBobSimple(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        // Create empty repository
        git = Git.init()
                 .setDirectory(tempDir.toFile())
                 .setInitialBranch("main")
                 .call();

        library = tempDir.resolve("library.bib");

        baseCommit = writeAndCommit(initialContent, "Inital commit", alice, library, git);

        aliceCommit = writeAndCommit(aliceUpdatedContent, "Fix author of a", alice, library, git);

        git.checkout().setStartPoint(baseCommit).setCreateBranch(true).setName("bob-branch").call();

        bobCommit = writeAndCommit(bobUpdatedContent, "Exchange a with b", bob, library, git);

        // ToDo: Replace by call to GitSyncService crafting a merge commit
//      git.merge().include(aliceCommit).include(bobCommit).call(); // Will throw exception bc of merge conflict

        // Debug hint: Show the created git graph on the command line
        //   git log --graph --oneline --decorate --all --reflog
    }

    @Test
    void performsSemanticMergeWhenNoConflicts() throws Exception {
    }

    @Test
    void readFromCommits() throws Exception {
        String base = GitFileReader.readFileFromCommit(git, baseCommit, Path.of("library.bib"));
        String local = GitFileReader.readFileFromCommit(git, aliceCommit, Path.of("library.bib"));
        String remote = GitFileReader.readFileFromCommit(git, bobCommit, Path.of("library.bib"));

        assertEquals(initialContent, base);
        assertEquals(aliceUpdatedContent, local);
        assertEquals(bobUpdatedContent, remote);
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author, Path library, Git git) throws Exception {
        Files.writeString(library, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(library.getFileName().toString()).call();
        return git.commit().setAuthor(author).setMessage(message).call();
    }
}
