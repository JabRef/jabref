package org.jabref.logic.git;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.git.util.GitFileReader;
import org.jabref.logic.git.util.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
              author = {don't know the author},
              doi = {xya},
            }

            @article{b,
              author = {don't know the author},
              doi = {xyz},
            }
        """;

    // Alice modifies a
    private final String aliceUpdatedContent = """
            @article{a,
              author = {author-a},
              doi = {xya},
            }

            @article{b,
              author = {don't know the author},
              doi = {xyz},
            }
        """;

    // Bob reorders a and b
    private final String bobUpdatedContent = """
            @article{b,
              author = {author-b},
              doi = {xyz},
            }

            @article{a,
              author = {don't know the author},
              doi = {xya},
            }
            """;


    /**
     * Creates a commit graph with a base commit, one modification by Alice and one modification by Bob
     * 1. Alice commit initial → push to remote
     * 2. Bob clone remote -> update `b` → push
     * 3. Alice update `a` → pull
     */
    @BeforeEach
    void aliceBobSimple(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        // create fake remote repo
        Path remoteDir = tempDir.resolve("remote.git");
        Git remoteGit = Git.init().setBare(true).setDirectory(remoteDir.toFile()).call();

        // Alice clone remote -> local repository
        Path aliceDir = tempDir.resolve("alice");
        Git aliceGit = Git.cloneRepository()
                          .setURI(remoteDir.toUri().toString())
                          .setDirectory(aliceDir.toFile())
                          .setBranch("main")
                          .call();
        this.git = aliceGit;
        this.library = aliceDir.resolve("library.bib");

        // Alice: initial commit
        baseCommit = writeAndCommit(initialContent, "Inital commit", alice, library, aliceGit);
        git.push().setRemote("origin").setRefSpecs(new RefSpec("main")).call();

        // Bob clone remote
        Path bobDir = tempDir.resolve("bob");
        Git bobGit = Git.cloneRepository()
                        .setURI(remoteDir.toUri().toString())
                        .setDirectory(bobDir.toFile())
                        .setBranchesToClone(List.of("refs/heads/main"))
                        .setBranch("refs/heads/main")
                        .call();
        Path bobLibrary = bobDir.resolve("library.bib");
        bobCommit = writeAndCommit(bobUpdatedContent, "Exchange a with b", bob, bobLibrary, bobGit);
        bobGit.push().setRemote("origin").setRefSpecs(new RefSpec("main")).call();

        // back to Alice's branch, fetch remote
        aliceCommit = writeAndCommit(aliceUpdatedContent, "Fix author of a", alice, library, aliceGit);
        git.fetch().setRemote("origin").call();

        // ToDo: Replace by call to GitSyncService crafting a merge commit
//      git.merge().include(aliceCommit).include(bobCommit).call(); // Will throw exception bc of merge conflict

        // Debug hint: Show the created git graph on the command line
        //   git log --graph --oneline --decorate --all --reflog
    }

    @Test
    void pullTriggersSemanticMergeWhenNoConflicts() throws Exception {
        GitHandler gitHandler = mock(GitHandler.class);
        GitSyncService syncService = new GitSyncService(importFormatPreferences, gitHandler);
        MergeResult result = syncService.fetchAndMerge(library);

        assertTrue(result.isSuccessful());
        String merged = Files.readString(library);

        String expected = """
        @article{a,
          author = {author-a},
          doi = {xya},
        }

        @article{b,
          author = {author-b},
          doi = {xyz},
        }
        """;

        assertEquals(normalize(expected), normalize(merged));
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
        String relativePath = git.getRepository().getWorkTree().toPath().relativize(library).toString();
        git.add().addFilepattern(relativePath).call();
        return git.commit()
                  .setAuthor(author)
                  .setMessage(message)
                  .call();
    }

    private String normalize(String s) {
        return s.trim()
                .replaceAll("@[aA]rticle", "@article")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }
}
