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
    private ImportFormatPreferences importFormatPreferences;
    private GitConflictResolverStrategy gitConflictResolverStrategy;

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
     * 2. Bob clone remote -> update b → push
     * 3. Alice update a → pull
     */
    @BeforeEach
    void aliceBobSimple(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        gitConflictResolverStrategy = mock(GitConflictResolverStrategy.class);

        // create fake remote repo
        Path remoteDir = tempDir.resolve("remote.git");
        Git remoteGit = Git.init()
                           .setBare(true)
                           .setInitialBranch("main")
                           .setDirectory(remoteDir.toFile())
                           .call();

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
        git.checkout()
           .setName("main")
           .call();

        git.push()
           .setRemote("origin")
           .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
           .call();

        Files.writeString(remoteDir.resolve("HEAD"), "ref: refs/heads/main");

        // Bob clone remote
        Path bobDir = tempDir.resolve("bob");
        Git bobGit = Git.cloneRepository()
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
        // Setup remote bare repo
        Path remoteDir = tempDir.resolve("remote.git");
        Git remoteGit = Git.init()
                           .setBare(true)
                           .setInitialBranch("main")
                           .setDirectory(remoteDir.toFile())
                           .call();
        Files.writeString(remoteDir.resolve("HEAD"), "ref: refs/heads/main");

        // Clone to local working directory
        Path localDir = tempDir.resolve("local");
        Git localGit = Git.cloneRepository()
                          .setURI(remoteDir.toUri().toString())
                          .setDirectory(localDir.toFile())
                          .setBranch("main")
                          .call();
        Path bibFile = localDir.resolve("library.bib");

        PersonIdent user = new PersonIdent("User", "user@example.com");

        String baseContent = """
        @article{a,
          author = {unknown},
          doi = {xya},
        }
        """;

        writeAndCommit(baseContent, "Initial commit", user, bibFile, localGit);

        localGit.checkout()
           .setName("main")
           .call();

        localGit.push()
                .setRemote("origin")
                .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                .call();

        // Clone again to simulate "remote user" making conflicting change
        Path remoteUserDir = tempDir.resolve("remoteUser");
        Git remoteUserGit = Git.cloneRepository()
                               .setURI(remoteDir.toUri().toString())
                               .setDirectory(remoteUserDir.toFile())
                               .setBranch("main")
                               .call();
        Path remoteUserFile = remoteUserDir.resolve("library.bib");

        String remoteContent = """
        @article{a,
          author = {remote-author},
          doi = {xya},
        }
        """;

        writeAndCommit(remoteContent, "Remote change", user, remoteUserFile, remoteUserGit);
        remoteUserGit.push()
                     .setRemote("origin")
                     .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                     .call();

        // Back to local, make conflicting change
        String localContent = """
        @article{a,
          author = {local-author},
          doi = {xya},
        }
        """;
        writeAndCommit(localContent, "Local change", user, bibFile, localGit);
        localGit.fetch().setRemote("origin").call();

        // Setup GitSyncService
        GitConflictResolverStrategy resolver = mock(GitConflictResolverStrategy.class);
        when(resolver.resolveConflicts(anyList(), any())).thenAnswer(invocation -> {
            List<ThreeWayEntryConflict> conflicts = invocation.getArgument(0);
            BibDatabaseContext remote = invocation.getArgument(1);

            BibEntry resolved = (BibEntry) conflicts.getFirst().base().clone();
            resolved.setField(StandardField.AUTHOR, "merged-author");

            BibDatabaseContext merged = GitMergeUtil.replaceEntries(remote, List.of(resolved));
            return Optional.of(merged);
        });

        GitHandler handler = new GitHandler(localDir);
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(prefs.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        GitSyncService service = new GitSyncService(prefs, handler, resolver);

        // Trigger semantic merge
        MergeResult result = service.fetchAndMerge(bibFile);

        assertTrue(result.isSuccessful());
        String finalContent = Files.readString(bibFile);
        assertTrue(finalContent.contains("merged-author"));
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
