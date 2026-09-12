package org.jabref.gui.whatsnew;

import java.io.IOException;
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The git checkout JabRef runs out of during development, read through JabRef's own [GitHandler] (JGit).
/// Every failure (no upstream, no network, no changelog at the revision) is an empty answer: the news is an
/// offer, never an error.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class CheckoutGit {

    /// The upstream of the checked-out branch, as `git` spells it; JGit's `resolve` does not know it.
    public static final String UPSTREAM = "@{u}";

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutGit.class);
    private static final String CHANGELOG = "CHANGELOG.md";
    private static final DateTimeFormatter COMMIT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GitHandler handler;

    private CheckoutGit(GitHandler handler) {
        this.handler = handler;
    }

    /// The checkout holding `start`, or empty for a packaged JabRef, which has nothing to update from.
    public static Optional<CheckoutGit> around(Path start, GitHandlerRegistry registry) {
        return registry.fromAnyPath(start).map(CheckoutGit::new);
    }

    /// The checkout's private git directory (per worktree), where this feature keeps its state files.
    public Optional<Path> gitDir() {
        try (Git git = handler.open()) {
            return Optional.of(git.getRepository().getDirectory().toPath());
        } catch (IOException e) {
            LOGGER.debug("Cannot open the checkout", e);
            return Optional.empty();
        }
    }

    /// How many commits the checkout is behind its upstream branch, after a fetch; 0 on any failure.
    /// The fetch is anonymous unless JabRef has git credentials configured — a public clone needs none.
    public int commitsBehind() {
        try (Git git = handler.open()) {
            FetchCommand fetch = git.fetch();
            handler.getCredentialsProvider().ifPresent(fetch::setCredentialsProvider);
            fetch.call();
            Repository repository = git.getRepository();
            @Nullable BranchTrackingStatus status = BranchTrackingStatus.of(repository, repository.getBranch());
            return status == null ? 0 : status.getBehindCount();
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("Cannot check how far the checkout is behind upstream", e);
            return 0;
        }
    }

    /// `rev` — `HEAD`, [#UPSTREAM] or anything `git rev-parse` takes — as an object id.
    private static @Nullable ObjectId resolve(Repository repository, String rev) throws IOException {
        if (!UPSTREAM.equals(rev)) {
            return repository.resolve(rev);
        }
        @Nullable String tracking = new BranchConfig(repository.getConfig(), repository.getBranch()).getTrackingBranch();
        return tracking == null ? null : repository.resolve(tracking);
    }

    /// `rev` (`HEAD`, [#UPSTREAM], …) as `<short sha> (<committer date and time>)`.
    public Optional<String> describe(String rev) {
        try (Git git = handler.open();
             RevWalk walk = new RevWalk(git.getRepository())) {
            @Nullable ObjectId id = resolve(git.getRepository(), rev);
            if (id == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(id);
            PersonIdent committer = commit.getCommitterIdent();
            String time = COMMIT_TIME.format(committer.getWhenAsInstant().atZone(ZoneId.systemDefault()));
            return Optional.of(id.abbreviate(7).name() + " (" + time + ")");
        } catch (IOException e) {
            LOGGER.debug("Cannot describe {}", rev, e);
            return Optional.empty();
        }
    }

    /// `CHANGELOG.md` blamed at `rev` (the working tree for an empty `rev`): its lines and, per line, who
    /// wrote it — `me` for a line of `user.email` or one not committed yet, the author's name otherwise.
    public Optional<WhatsNew.Source> blame(String rev, String me) {
        try (Git git = handler.open()) {
            Repository repository = git.getRepository();
            BlameCommand blame = git.blame().setFilePath(CHANGELOG);
            if (!rev.isEmpty()) {
                @Nullable ObjectId id = resolve(repository, rev);
                if (id == null) {
                    return Optional.empty();
                }
                blame.setStartCommit(id);
            }
            @Nullable BlameResult result = blame.call();
            if (result == null) {
                return Optional.empty();
            }
            String mail = Optional.ofNullable(repository.getConfig().getString("user", null, "email")).orElse("");
            List<String> lines = new ArrayList<>();
            List<String> by = new ArrayList<>();
            for (int i = 0; i < result.getResultContents().size(); i++) {
                lines.add(result.getResultContents().getString(i));
                @Nullable RevCommit source = result.getSourceCommit(i);
                @Nullable PersonIdent author = result.getSourceAuthor(i);
                boolean mine = source == null || author == null || author.getEmailAddress().equalsIgnoreCase(mail);
                by.add(mine ? me : author.getName());
            }
            return Optional.of(new WhatsNew.Source(lines, by));
        } catch (IOException | GitAPIException e) {
            LOGGER.debug("Cannot blame {} at {}", CHANGELOG, rev, e);
            return Optional.empty();
        }
    }
}
