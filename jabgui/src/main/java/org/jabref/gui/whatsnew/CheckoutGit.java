package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The git checkout JabRef runs out of during development, asked through the `git` on the `PATH`.
/// Every failure (no git, no upstream, no network) is an empty answer: the news is an offer, never an error.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class CheckoutGit {

    /// `git fetch` talks to the network; nothing waits on it.
    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutGit.class);

    private final Path repo;

    private CheckoutGit(Path repo) {
        this.repo = repo;
    }

    /// The checkout holding `start`: the nearest ancestor with a `.git` (a directory in a clone, a file in a worktree).
    /// Empty for a packaged JabRef, which has nothing to update from.
    public static Optional<CheckoutGit> around(Path start) {
        for (Path dir = start.toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve(".git"))) {
                return Optional.of(new CheckoutGit(dir));
            }
        }
        return Optional.empty();
    }

    /// The checkout's private git directory (per worktree), where this feature keeps its state files.
    public Optional<Path> gitDir() {
        return run("rev-parse", "--absolute-git-dir").flatMap(lines -> lines.stream().findFirst()).map(Path::of);
    }

    /// How many commits the checkout is behind its upstream branch, after a fetch; 0 on any failure.
    public int commitsBehind() {
        if (run("fetch", "--quiet").isEmpty()) {
            return 0;
        }
        return run("rev-list", "--count", "HEAD..@{u}")
                .flatMap(lines -> lines.stream().findFirst())
                .map(String::strip)
                .map(count -> {
                    try {
                        return Integer.parseInt(count);
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Cannot read the commit count behind upstream: {}", count);
                        return 0;
                    }
                })
                .orElse(0);
    }

    /// `commit` as `<short sha> (<committer date and time>)`.
    public Optional<String> describe(String commit) {
        return run("show", "--no-patch", "--date=format:%Y-%m-%d %H:%M", "--format=%h (%cd)", commit)
                .flatMap(lines -> lines.stream().findFirst())
                .map(String::strip)
                .filter(text -> !text.isEmpty());
    }

    /// `CHANGELOG.md` blamed at `rev` (the working tree for an empty `rev`), every line of `user.email`
    /// or not committed yet marked as written by `me`.
    public Optional<WhatsNew.Source> blame(String rev, String me) {
        Optional<List<String>> porcelain = rev.isEmpty()
                ? run("blame", "--line-porcelain", "--", "CHANGELOG.md")
                : run("blame", "--line-porcelain", rev, "--", "CHANGELOG.md");
        String mail = run("config", "user.email").flatMap(lines -> lines.stream().findFirst()).map(String::strip).orElse("");
        return porcelain.map(lines -> WhatsNew.parse(lines, mail, me));
    }

    private Optional<List<String>> run(String... args) {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            List<String> output = process.inputReader().lines().toList();
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                LOGGER.debug("git {} timed out", String.join(" ", args));
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                LOGGER.debug("git {} exited with {}", String.join(" ", args), process.exitValue());
                return Optional.empty();
            }
            return Optional.of(output);
        } catch (IOException e) {
            LOGGER.debug("Cannot run git {}", String.join(" ", args), e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
