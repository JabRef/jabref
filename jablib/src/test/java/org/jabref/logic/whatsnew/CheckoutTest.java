package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.git.util.NoopGitSystemReader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A clone of an upstream repository, both on disk: the upstream holds a changelog by somebody else, the clone
/// is configured with my e-mail address.
// [utest->req~whats-new.checkout-news~1]
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("git")
class CheckoutTest {

    private static final String MY_EMAIL = "me@example.org";
    private static final String CHANGELOG = "CHANGELOG.md";
    private static final List<String> INITIAL_CHANGELOG = List.of("## [Unreleased]", "### Added", "- An entry by somebody else.");
    private static final Pattern DESCRIPTION = Pattern.compile("[0-9a-f]{7} \\(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\)");

    @TempDir
    Path upstreamDirectory;
    @TempDir
    Path cloneDirectory;

    private Checkout checkout;

    @BeforeEach
    void cloneAnUpstreamWithAChangelog() throws IOException, GitAPIException {
        SystemReader.setInstance(new NoopGitSystemReader());
        try (Git upstream = Git.init().setInitialBranch("main").setDirectory(upstreamDirectory.toFile()).call()) {
            commit(upstream, INITIAL_CHANGELOG, "Somebody Else", "somebody@example.org");
        }
        try (Git clone = Git.cloneRepository().setURI(upstreamDirectory.toUri().toString()).setDirectory(cloneDirectory.toFile()).call()) {
            clone.getRepository().getConfig().setString("user", null, "email", MY_EMAIL);
            clone.getRepository().getConfig().save();
        }
        checkout = Checkout.around(cloneDirectory.resolve("some").resolve("subdirectory"), new GitHandlerRegistry(GitPreferences.getDefault())).orElseThrow();
    }

    @AfterEach
    void releaseJGitCaches() {
        // Required by JGit, see https://github.com/eclipse-jgit/jgit/issues/155#issuecomment-2765437816
        RepositoryCache.clear();
        WindowCache.reconfigure(new WindowCacheConfig());
    }

    private static void commit(Git git, List<String> changelog, String author, String email) throws IOException, GitAPIException {
        Files.write(git.getRepository().getWorkTree().toPath().resolve(CHANGELOG), changelog);
        git.add().addFilepattern(CHANGELOG).call();
        git.commit().setMessage("Update changelog").setAuthor(author, email).setCommitter(author, email).call();
    }

    @Test
    void aDirectoryOutsideAnyRepositoryIsNoCheckout(@TempDir Path elsewhere) {
        assertEquals(Optional.empty(), Checkout.around(elsewhere, new GitHandlerRegistry(GitPreferences.getDefault())));
    }

    @Test
    void gitDirIsTheClonesOwn() throws IOException {
        assertEquals(cloneDirectory.resolve(".git").toRealPath(), checkout.gitDir().orElseThrow().toRealPath());
    }

    @Test
    void theWorkingTreeBlamesUncommittedLinesOnMe() throws IOException {
        Files.write(cloneDirectory.resolve(CHANGELOG), List.of("## [Unreleased]", "### Added", "- An entry by somebody else.", "- An entry I am writing."));

        BlamedChangelog blamed = checkout.blameWorkingTree().orElseThrow();

        assertEquals(List.of(
                new BlamedChangelog.Line("## [Unreleased]", new Contributor.Other("Somebody Else")),
                new BlamedChangelog.Line("### Added", new Contributor.Other("Somebody Else")),
                new BlamedChangelog.Line("- An entry by somebody else.", new Contributor.Other("Somebody Else")),
                new BlamedChangelog.Line("- An entry I am writing.", Contributor.Me.LOCAL)), blamed.lines());
    }

    @Test
    void aChangelogWithWindowsLineEndingsIsBlamedWithoutThem() throws IOException {
        Files.writeString(cloneDirectory.resolve(CHANGELOG), String.join("\r\n", "## [Unreleased]", "### Added", "- An entry I am writing."));

        BlamedChangelog blamed = checkout.blameWorkingTree().orElseThrow();

        assertEquals(List.of("## [Unreleased]", "### Added", "- An entry I am writing."), blamed.lines().stream().map(BlamedChangelog.Line::text).toList());
    }

    @Test
    void aFreshCloneIsNotBehind() {
        assertTrue(checkout.fetch());

        assertEquals(0, checkout.commitsBehind());
        assertEquals(checkout.describeHead(), checkout.describeUpstream());
    }

    @Test
    void aCommitPushedFromElsewhereIsBehindAndBlamedOnMeRemotely() throws IOException, GitAPIException {
        try (Git upstream = Git.open(upstreamDirectory.toFile())) {
            commit(upstream, List.of("## [Unreleased]", "### Added", "- An entry by somebody else.", "- An entry I pushed from elsewhere."), "Me", MY_EMAIL);
        }

        checkout.fetch();

        assertEquals(1, checkout.commitsBehind());
        assertNotEquals(checkout.describeHead(), checkout.describeUpstream());
        assertTrue(DESCRIPTION.matcher(checkout.describeUpstream().orElseThrow()).matches());
        assertEquals(new BlamedChangelog.Line("- An entry I pushed from elsewhere.", Contributor.Me.REMOTE),
                checkout.blameUpstream().orElseThrow().lines().getLast());
    }
}
