package org.jabref.logic.git;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.merge.GitMergeUtil;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitSyncServiceTest {
    private Git git;
    private Path library;
    private Path remoteDir;
    private Path aliceDir;
    private Path bobDir;
    private Git aliceGit;
    private Git bobGit;
    private ImportFormatPreferences importFormatPreferences;
    private GitConflictResolverStrategy gitConflictResolverStrategy;

    // These are setup by aliceBobSetting
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
     * 2. Bob clone remote -> update b → push
     * 3. Alice update a → pull
     */
    @BeforeEach
    void aliceBobSimple(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        gitConflictResolverStrategy = mock(GitConflictResolverStrategy.class);

        // create fake remote repo
        remoteDir = tempDir.resolve("remote.git");
        Git remoteGit = Git.init()
                           .setBare(true)
                           .setInitialBranch("main")
                           .setDirectory(remoteDir.toFile())
                           .call();
        remoteGit.close();

        // Alice clone remote -> local repository
        aliceDir = tempDir.resolve("alice");
        aliceGit = Git.cloneRepository()
                          .setURI(remoteDir.toUri().toString())
                          .setDirectory(aliceDir.toFile())
                          .call();

        this.git = aliceGit;
        this.library = aliceDir.resolve("library.bib");
        // Initial commit
        baseCommit = writeAndCommit(initialContent, "Initial commit", alice, library, aliceGit);

        git.push()
           .setRemote("origin")
           .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
           .call();

        aliceGit.checkout()
                .setName("main")
                .call();

        // Bob clone remote
        bobDir = tempDir.resolve("bob");
        bobGit = Git.cloneRepository()
                        .setURI(remoteDir.toUri().toString())
                        .setDirectory(bobDir.toFile())
                        .setBranchesToClone(List.of("refs/heads/main"))
                        .setBranch("main")
                        .call();
        Path bobLibrary = bobDir.resolve("library.bib");
        bobCommit = writeAndCommit(bobUpdatedContent, "Exchange a with b", bob, bobLibrary, bobGit);
        bobGit.push()
              .setRemote("origin")
              .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
              .call();

        // back to Alice's branch, fetch remote
        aliceCommit = writeAndCommit(aliceUpdatedContent, "Fix author of a", alice, library, aliceGit);
        git.fetch().setRemote("origin").call();

        // Debug hint: Show the created git graph on the command line
        //   git log --graph --oneline --decorate --all --reflog
    }

    @Test
    void pullTriggersSemanticMergeWhenNoConflicts() throws Exception {
        GitHandler gitHandler = new GitHandler(library.getParent());
        GitSyncService syncService = new GitSyncService(importFormatPreferences, gitHandler, gitConflictResolverStrategy);
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
    void pushTriggersMergeAndPushWhenNoConflicts() throws Exception {
        GitHandler gitHandler = new GitHandler(library.getParent());
        GitSyncService syncService = new GitSyncService(importFormatPreferences, gitHandler, gitConflictResolverStrategy);
        syncService.push(library);

        String pushedContent = GitFileReader.readFileFromCommit(git, git.log().setMaxCount(1).call().iterator().next(), Path.of("library.bib"));
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

        assertEquals(normalize(expected), normalize(pushedContent));
    }

    @Test
    void mergeConflictOnSameFieldTriggersDialogAndUsesUserResolution(@TempDir Path tempDir) throws Exception {
        // Bob adds entry c
        Path bobLibrary = bobDir.resolve("library.bib");
        String bobEntry = """
            @article{b,
              author = {author-b},
              doi = {xyz},
            }
            @article{a,
              author = {author-a},
              doi = {xya},
            }
            @article{c,
              author = {bob-c},
              title = {Title C},
            }
            """;
        writeAndCommit(bobEntry, "Bob adds article-c", bob, bobLibrary, bobGit);
        bobGit.push().setRemote("origin").call();
        // Alice adds conflicting version of c
        String aliceEntry = """
            @article{b,
              author = {author-b},
              doi = {xyz},
            }
            @article{a,
              author = {author-a},
              doi = {xya},
            }
            @article{c,
                author = {alice-c},
                title = {Title C},
            }
        """;
        writeAndCommit(aliceEntry, "Alice adds conflicting article-c", alice, library, aliceGit);
        git.fetch().setRemote("origin").call();

        // Setup mock conflict resolver
        GitConflictResolverStrategy resolver = mock(GitConflictResolverStrategy.class);
        when(resolver.resolveConflicts(anyList(), any())).thenAnswer(invocation -> {
            List<ThreeWayEntryConflict> conflicts = invocation.getArgument(0);
            BibDatabaseContext remote = invocation.getArgument(1);

            ThreeWayEntryConflict conflict = ((List<ThreeWayEntryConflict>) invocation.getArgument(0)).getFirst();
            // In this test, both Alice and Bob independently added a new entry 'c', so the base is null.
            // We simulate conflict resolution by choosing the remote version and modifying the author field.
            BibEntry resolved = ((BibEntry) conflict.remote().clone());
            resolved.setField(StandardField.AUTHOR, "alice-c + bob-c");

            BibDatabaseContext merged = GitMergeUtil.replaceEntries(remote, List.of(resolved));
            return Optional.of(merged);
        });

        GitHandler handler = new GitHandler(aliceDir);
        GitSyncService service = new GitSyncService(importFormatPreferences, handler, resolver);
        MergeResult result = service.fetchAndMerge(library);

        assertTrue(result.isSuccessful());
        String content = Files.readString(library);
        assertTrue(content.contains("alice-c + bob-c"));
        verify(resolver).resolveConflicts(anyList(), any());
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

    @AfterEach
    void cleanup() {
        if (git != null) {
            git.close();
        }
        if (aliceGit != null && aliceGit != git) {
            aliceGit.close();
        }
        if (bobGit != null) {
            bobGit.close();
        }
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
