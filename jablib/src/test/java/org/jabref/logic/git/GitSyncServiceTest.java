package org.jabref.logic.git;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.git.merge.GitSemanticMergeExecutor;
import org.jabref.logic.git.merge.GitSemanticMergeExecutorImpl;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitSyncServiceTest {
    private Path library;
    private Path remoteDir;
    private Path aliceDir;
    private Path bobDir;
    private Git aliceGit;
    private Git bobGit;
    private ImportFormatPreferences importFormatPreferences;
    private GitConflictResolverStrategy gitConflictResolverStrategy;
    private GitSemanticMergeExecutor mergeExecutor;
    private BibDatabaseContext context;

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
        mergeExecutor = new GitSemanticMergeExecutorImpl(importFormatPreferences);

        // create fake remote repo
        remoteDir = tempDir.resolve("remote.git");
        Git remoteGit = Git.init()
                           .setBare(true)
                           .setInitialBranch("main")
                           .setDirectory(remoteDir.toFile())
                           .call();
        remoteGit.close();

        // Alice init local repository
        aliceDir = tempDir.resolve("alice");
        aliceGit = Git.init()
                      .setInitialBranch("main")
                      .setDirectory(aliceDir.toFile())
                      .call();

        this.library = aliceDir.resolve("library.bib");

        // Initial commit
        baseCommit = writeAndCommit(initialContent, "Initial commit", alice, library, aliceGit);
        // Add remote and push to create refs/heads/main in remote
        aliceGit.remoteAdd()
                .setName("origin")
                .setUri(new URIish(remoteDir.toUri().toString()))
                .call();

        aliceGit.push()
           .setRemote("origin")
           .setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
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
        aliceGit.fetch().setRemote("origin").call();

        String actualContent = Files.readString(library);
        ParserResult parsed = new BibtexParser(importFormatPreferences).parse(Reader.of(actualContent));
        context = new BibDatabaseContext(parsed.getDatabase(), parsed.getMetaData());
        context.setDatabasePath(library);

        // Debug hint: Show the created git graph on the command line
        //   git log --graph --oneline --decorate --all --reflog
    }

    @Test
    void pullTriggersSemanticMergeWhenNoConflicts() throws Exception {
        GitHandler gitHandler = new GitHandler(library.getParent());
        GitSyncService syncService = new GitSyncService(importFormatPreferences, gitHandler, gitConflictResolverStrategy, mergeExecutor);
        MergeResult result = syncService.fetchAndMerge(context, library);

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
        GitSyncService syncService = new GitSyncService(importFormatPreferences, gitHandler, gitConflictResolverStrategy, mergeExecutor);
        syncService.push(context, library);

        String pushedContent = GitFileReader
                .readFileFromCommit(aliceGit, aliceGit.log().setMaxCount(1).call().iterator().next(), Path.of("library.bib"))
                .orElseThrow(() -> new IllegalStateException("Expected file 'library.bib' not found in commit"));
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
        Path bobLibrary = bobDir.resolve("library.bib");
        String bobEntry = """
              @article{b,
              author = {author-b},
              doi = {xyz},
            }

            @article{a,
              author = {don't know the author},
              doi = {xya},
            }

            @article{c,
              author = {bob-c},
              title = {Title C},
            }
        """;
        writeAndCommit(bobEntry, "Bob adds article-c", bob, bobLibrary, bobGit);
        bobGit.push().setRemote("origin").call();
        String aliceEntry = """
            @article{a,
              author = {author-a},
              doi = {xya},
            }

            @article{b,
              author = {don't know the author},
              doi = {xyz},
            }

            @article{c,
              author = {alice-c},
              title = {Title C},
            }
        """;
        writeAndCommit(aliceEntry, "Alice adds conflicting article-c", alice, library, aliceGit);
        aliceGit.fetch().setRemote("origin").call();

        String actualContent = Files.readString(library);
        ParserResult parsed = new BibtexParser(importFormatPreferences).parse(Reader.of(actualContent));
        context = new BibDatabaseContext(parsed.getDatabase(), parsed.getMetaData());
        context.setDatabasePath(library);

        // Setup mock conflict resolver
        GitConflictResolverStrategy resolver = mock(GitConflictResolverStrategy.class);
        when(resolver.resolveConflicts(anyList())).thenAnswer(invocation -> {
            List<ThreeWayEntryConflict> conflicts = invocation.getArgument(0);
            ThreeWayEntryConflict conflict = conflicts.getFirst();
            // In this test, both Alice and Bob independently added a new entry 'c', so the base is null.
            // We simulate conflict resolution by choosing the remote version and modifying the author field.
            BibEntry resolved = (BibEntry) conflict.remote().clone();
            resolved.setField(StandardField.AUTHOR, "alice-c + bob-c");
            return Optional.of(List.of(resolved));
        });

        GitHandler handler = new GitHandler(aliceDir);
        GitSyncService service = new GitSyncService(importFormatPreferences, handler, resolver, mergeExecutor);
        MergeResult result = service.fetchAndMerge(context, library);

        assertTrue(result.isSuccessful());
        String content = Files.readString(library);
        assertTrue(content.contains("alice-c + bob-c"));
        verify(resolver).resolveConflicts(anyList());
    }

    @Test
    void readFromCommits() throws Exception {
        String base = GitFileReader
                .readFileFromCommit(aliceGit, baseCommit, Path.of("library.bib"))
                .orElseThrow(() -> new IllegalStateException("Base version of library.bib not found"));

        String local = GitFileReader
                .readFileFromCommit(aliceGit, aliceCommit, Path.of("library.bib"))
                .orElseThrow(() -> new IllegalStateException("Local version of library.bib not found"));

        String remote = GitFileReader
                .readFileFromCommit(aliceGit, bobCommit, Path.of("library.bib"))
                .orElseThrow(() -> new IllegalStateException("Remote version of library.bib not found"));

        assertEquals(initialContent, base);
        assertEquals(aliceUpdatedContent, local);
        assertEquals(bobUpdatedContent, remote);
    }

    @AfterEach
    void cleanup() {
        if (aliceGit != null) {
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
