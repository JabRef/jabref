package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

/// The news of a checkout: one look reads the checkout and projects the entries not announced yet; once they
/// were shown, [#announce] makes them old. What a toolbar button, a window or anything else shows comes from here.
// [impl->req~whats-new.checkout-news~1]
public final class CheckoutNews {

    /// What one look found.
    ///
    /// @param fetched       whether the upstream was reached; without it, `commitsBehind` is from the last fetch
    /// @param commitsBehind how many commits the checkout is behind its upstream
    /// @param head          the commit checked out, described, while behind
    /// @param upstream      the upstream commit, described, while behind
    /// @param news          the entries not announced yet
    /// @param seen          every entry the look passed by, news or not: what [#announce] makes old
    public record Look(boolean fetched, int commitsBehind, Optional<String> head, Optional<String> upstream, News news, SequencedSet<ChangelogEntry> seen) {
    }

    /// Whether a look fetches the upstream first.
    public enum Mode {
        /// Right after `just run-loop` pulled: the upstream is as fresh as it gets.
        WITHOUT_FETCH,
        /// The periodic check.
        WITH_FETCH
    }

    private final Checkout checkout;
    private final AnnouncedEntries announced;

    public CheckoutNews(Checkout checkout, AnnouncedEntries announced) {
        this.checkout = checkout;
        this.announced = announced;
    }

    /// Reads the checkout: the working tree's changelog, plus the upstream's once the checkout is behind, so an
    /// entry arriving upstream while a local edit is pending hides nothing. Without announced entries yet (the
    /// first look in a checkout) everything seen is announced now and nothing is news: a fresh checkout is not
    /// greeted with the whole changelog — but only once a changelog could be read, or a failed first look would
    /// announce nothing and the next one everything.
    ///
    /// Looks and announcements run one after the other and so answer in the order they started: an older look
    /// never overwrites a newer one, on disk or on screen. Blocking: not for the UI thread.
    public synchronized Look look(Mode mode) throws IOException {
        boolean fetched = mode == Mode.WITH_FETCH && checkout.fetch();
        int behind = checkout.commitsBehind();
        List<BlamedChangelog> changelogs = new ArrayList<>();
        checkout.blameWorkingTree().ifPresent(changelogs::add);
        if (behind > 0) {
            checkout.blameUpstream().ifPresent(changelogs::add);
        }
        Optional<String> head = behind > 0 ? checkout.describeHead() : Optional.empty();
        Optional<String> upstream = behind > 0 ? checkout.describeUpstream() : Optional.empty();
        SequencedSet<ChangelogEntry> seen = News.allEntries(changelogs);
        if (changelogs.isEmpty()) {
            return new Look(fetched, behind, head, upstream, News.NONE, seen);
        }
        Optional<Set<ChangelogEntry>> announcedSoFar = announced.read();
        News news = announcedSoFar.map(old -> News.pending(old, changelogs)).orElse(News.NONE);
        if (announcedSoFar.isEmpty()) {
            announced.write(seen);
        }
        return new Look(fetched, behind, head, upstream, news, seen);
    }

    /// Makes everything `look` saw old: called once its news were shown, so a window closed before its answer
    /// leaves the news pending. Blocking: not for the UI thread.
    public synchronized void announce(Look look) throws IOException {
        announced.write(look.seen());
    }
}
