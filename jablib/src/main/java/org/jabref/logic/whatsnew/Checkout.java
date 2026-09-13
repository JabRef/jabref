package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.util.GitHandlerRegistry;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The git checkout JabRef runs out of during development, read through JabRef's own [GitHandler] (JGit).
///
/// Every question is answered with an empty result on failure (no upstream, no network, no changelog at the
/// revision) and the cause is logged: the news is an offer to the developer, never an error to show.
// [impl->req~whats-new.checkout-news~1]
public final class Checkout {

    private static final Logger LOGGER = LoggerFactory.getLogger(Checkout.class);
    private static final String CHANGELOG = "CHANGELOG.md";

    /// What tells JabRef's own source tree from any other repository JabRef happens to be started in.
    private static final Path JABGUI_BUILD_FILE = Path.of("jabgui", "build.gradle.kts");
    private static final int ABBREVIATED_ID_LENGTH = 7;
    private static final DateTimeFormatter COMMIT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GitHandler handler;

    private Checkout(GitHandler handler) {
        this.handler = handler;
    }

    /// The JabRef source checkout holding `anyPathInside`; empty for a packaged JabRef, which runs out of no
    /// checkout, and for a repository that is not JabRef's.
    public static Optional<Checkout> around(Path anyPathInside, GitHandlerRegistry registry) {
        return registry.fromAnyPath(anyPathInside)
                       .filter(handler -> Files.exists(handler.getRepositoryPathAsFile().toPath().resolve(JABGUI_BUILD_FILE)))
                       .map(Checkout::new);
    }

    /// The checkout's private git directory (`.git`, or the worktree's directory under it).
    public Optional<Path> gitDir() {
        try (Git git = handler.open()) {
            return Optional.of(git.getRepository().getDirectory().toPath());
        } catch (IOException e) {
            LOGGER.debug("Cannot open the checkout", e);
            return Optional.empty();
        }
    }

    /// Fetches from the upstream of the checked-out branch; `false` when that failed (offline, no remote), in
    /// which case [#commitsBehind()] answers from the last fetch that succeeded. The fetch is anonymous unless
    /// JabRef has git credentials configured; a public clone needs none, which is why
    /// [GitHandler#fetchOnCurrentBranch()], which insists on credentials for every `https` remote, is not used.
    public boolean fetch() {
        try (Git git = handler.open()) {
            FetchCommand fetch = git.fetch();
            handler.getCredentialsProvider().ifPresent(fetch::setCredentialsProvider);
            fetch.call();
            return true;
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("Cannot fetch the checkout's upstream", e);
            return false;
        }
    }

    /// How many commits the checked-out branch is behind its upstream, as last fetched; 0 without an upstream.
    public int commitsBehind() {
        try (Git git = handler.open()) {
            Repository repository = git.getRepository();
            @Nullable BranchTrackingStatus status = BranchTrackingStatus.of(repository, repository.getBranch());
            return status == null ? 0 : status.getBehindCount();
        } catch (IOException e) {
            LOGGER.debug("Cannot compare the checkout with its upstream", e);
            return 0;
        }
    }

    /// The commit checked out, as `<abbreviated id> (<commit date and time>)`.
    public Optional<String> describeHead() {
        return describe(Revision.HEAD);
    }

    /// The upstream of the checked-out branch, as `<abbreviated id> (<commit date and time>)`.
    public Optional<String> describeUpstream() {
        return describe(Revision.UPSTREAM);
    }

    /// `CHANGELOG.md` as it is in the working tree; a line not committed yet is mine.
    public Optional<BlamedChangelog> blameWorkingTree() {
        try (Git git = handler.open()) {
            return blame(git.getRepository(), git.blame().setFilePath(CHANGELOG), Contributor.Me.LOCAL);
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("Cannot blame {} in the working tree", CHANGELOG, e);
            return Optional.empty();
        }
    }

    /// `CHANGELOG.md` as the upstream of the checked-out branch has it, as last fetched.
    public Optional<BlamedChangelog> blameUpstream() {
        try (Git git = handler.open()) {
            Optional<ObjectId> upstream = Revision.UPSTREAM.resolve(git.getRepository());
            if (upstream.isEmpty()) {
                return Optional.empty();
            }
            BlameCommand blame = git.blame().setFilePath(CHANGELOG).setStartCommit(upstream.get());
            return blame(git.getRepository(), blame, Contributor.Me.REMOTE);
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("Cannot blame {} at the upstream", CHANGELOG, e);
            return Optional.empty();
        }
    }

    /// The commits this class describes, resolved the way `git rev-parse` would: JGit's `resolve` takes `HEAD`
    /// but does not know `@{u}`.
    private enum Revision {
        HEAD {
            @Override
            Optional<ObjectId> resolve(Repository repository) throws IOException {
                return Optional.ofNullable(repository.resolve(Constants.HEAD));
            }
        },
        UPSTREAM {
            @Override
            Optional<ObjectId> resolve(Repository repository) throws IOException {
                @Nullable String tracking = new BranchConfig(repository.getConfig(), repository.getBranch()).getTrackingBranch();
                return tracking == null ? Optional.empty() : Optional.ofNullable(repository.resolve(tracking));
            }
        };

        abstract Optional<ObjectId> resolve(Repository repository) throws IOException;
    }

    private Optional<String> describe(Revision revision) {
        try (Git git = handler.open();
             RevWalk walk = new RevWalk(git.getRepository())) {
            Optional<ObjectId> id = revision.resolve(git.getRepository());
            if (id.isEmpty()) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(id.get());
            PersonIdent committer = commit.getCommitterIdent();
            String time = COMMIT_TIME.format(committer.getWhenAsInstant().atZone(ZoneId.systemDefault()));
            return Optional.of(id.get().abbreviate(ABBREVIATED_ID_LENGTH).name() + " (" + time + ")");
        } catch (IOException e) {
            LOGGER.debug("Cannot describe {}", revision, e);
            return Optional.empty();
        }
    }

    /// Runs `blame`; every line is `mine` when not committed yet or committed under the checkout's
    /// `user.email`, and the author's otherwise.
    private static Optional<BlamedChangelog> blame(Repository repository, BlameCommand blame, Contributor.Me mine) throws GitAPIException {
        @Nullable BlameResult result = blame.call();
        if (result == null) {
            return Optional.empty();
        }
        String myEmail = Optional.ofNullable(repository.getConfig().getString("user", null, "email")).orElse("");
        List<BlamedChangelog.Line> lines = new ArrayList<>();
        for (int i = 0; i < result.getResultContents().size(); i++) {
            @Nullable PersonIdent author = result.getSourceCommit(i) == null ? null : result.getSourceAuthor(i);
            Contributor by = author == null || author.getEmailAddress().equalsIgnoreCase(myEmail)
                             ? mine
                             : new Contributor.Other(author.getName());
            lines.add(new BlamedChangelog.Line(withoutCarriageReturn(result.getResultContents().getString(i)), by));
        }
        return Optional.of(new BlamedChangelog(lines));
    }

    /// JGit ends a line at `\n` only, so a `\r\n` file (a Windows checkout) leaves the `\r` on every line.
    private static String withoutCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
