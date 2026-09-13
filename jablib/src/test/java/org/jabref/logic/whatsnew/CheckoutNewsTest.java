package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->req~whats-new.checkout-news~1]
class CheckoutNewsTest {

    private static final ChangelogEntry OLD = new ChangelogEntry("Unreleased", "Added", "An old entry.");
    private static final ChangelogEntry MINE = new ChangelogEntry("Unreleased", "Added", "An entry of mine.");
    private static final ChangelogEntry PUSHED = new ChangelogEntry("Unreleased", "Fixed", "An entry pushed from elsewhere.");

    @TempDir
    Path gitDir;

    private final Checkout checkout = mock(Checkout.class);
    private AnnouncedEntries announced;
    private CheckoutNews news;

    private static BlamedChangelog changelog(Contributor by, ChangelogEntry... entries) {
        List<BlamedChangelog.Line> lines = new ArrayList<>();
        for (ChangelogEntry entry : entries) {
            lines.add(new BlamedChangelog.Line("## [" + entry.section() + "]", by));
            lines.add(new BlamedChangelog.Line("### " + entry.heading(), by));
            lines.add(new BlamedChangelog.Line("- " + entry.text(), by));
        }
        return new BlamedChangelog(lines);
    }

    @BeforeEach
    void setUp() {
        when(checkout.fetch()).thenReturn(true);
        when(checkout.blameWorkingTree()).thenReturn(Optional.of(changelog(Contributor.Me.LOCAL, OLD, MINE)));
        announced = AnnouncedEntries.inGitDir(gitDir);
        news = new CheckoutNews(checkout, announced);
    }

    @Test
    void theFirstLookAnnouncesEverythingSilently() throws IOException {
        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITHOUT_FETCH);

        assertEquals(News.NONE, look.news());
        assertEquals(Optional.of(Set.of(OLD, MINE)), announced.read());
    }

    @Test
    void aFirstLookWithoutAChangelogAnnouncesNothing() throws IOException {
        when(checkout.blameWorkingTree()).thenReturn(Optional.empty());

        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITHOUT_FETCH);

        assertEquals(News.NONE, look.news());
        assertEquals(Optional.empty(), announced.read());
    }

    @Test
    void aLaterLookFindsWhatWasNotAnnounced() throws IOException {
        announced.announce(Set.of(OLD));

        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITHOUT_FETCH);

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, MINE))), look.news());
        assertEquals(List.of(OLD, MINE), List.copyOf(look.seen()));
        assertEquals(Optional.of(Set.of(OLD)), announced.read());
    }

    @Test
    void behindTheUpstreamTheFetchedChangelogCountsToo() throws IOException {
        announced.announce(Set.of(OLD, MINE));
        when(checkout.commitsBehind()).thenReturn(2);
        when(checkout.blameUpstream()).thenReturn(Optional.of(changelog(Contributor.Me.REMOTE, OLD, PUSHED)));
        when(checkout.describeHead()).thenReturn(Optional.of("1111111 (2026-09-13 10:00)"));
        when(checkout.describeUpstream()).thenReturn(Optional.of("2222222 (2026-09-13 11:00)"));

        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITH_FETCH);

        assertTrue(look.fetched());
        assertEquals(2, look.commitsBehind());
        assertEquals(Optional.of("1111111 (2026-09-13 10:00)"), look.head());
        assertEquals(Optional.of("2222222 (2026-09-13 11:00)"), look.upstream());
        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.REMOTE, PUSHED))), look.news());
        assertEquals(List.of(OLD, MINE, PUSHED), List.copyOf(look.seen()));
    }

    @Test
    void anUnreachableUpstreamIsReported() throws IOException {
        announced.announce(Set.of(OLD));
        when(checkout.fetch()).thenReturn(false);

        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITH_FETCH);

        assertFalse(look.fetched());
        assertEquals(Optional.of(Set.of(OLD)), announced.read());
    }

    @Test
    void aLookWithoutAChangelogAnnouncesNothingEvenWhenAsked() throws IOException {
        announced.announce(Set.of(OLD));
        when(checkout.blameWorkingTree()).thenReturn(Optional.empty());

        news.announce(news.look(CheckoutNews.Mode.WITH_FETCH));

        assertEquals(Optional.of(Set.of(OLD)), announced.read());
    }

    @Test
    void aLookAnnouncesNothingUntilAsked() throws IOException {
        announced.announce(Set.of(OLD));

        CheckoutNews.Look look = news.look(CheckoutNews.Mode.WITH_FETCH);
        assertEquals(Optional.of(Set.of(OLD)), announced.read());

        news.announce(look);
        assertEquals(Optional.of(Set.of(OLD, MINE)), announced.read());
    }
}
