package org.jabref.logic.whatsnew;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~whats-new.checkout-news~1]
class NewsTest {

    private static final Contributor ALICE = new Contributor.Other("Alice");
    private static final Contributor BOB = new Contributor.Other("Bob");

    private static final ChangelogEntry ADDED = new ChangelogEntry("Unreleased", "Added", "added");
    private static final ChangelogEntry FIXED = new ChangelogEntry("Unreleased", "Fixed", "fixed");
    private static final ChangelogEntry BRAND_NEW = new ChangelogEntry("Unreleased", "Added", "brand new");

    private static BlamedChangelog changelog(Contributor by, ChangelogEntry... entries) {
        List<BlamedChangelog.Line> lines = new ArrayList<>();
        for (ChangelogEntry entry : entries) {
            lines.add(new BlamedChangelog.Line("## [" + entry.section() + "]", by));
            lines.add(new BlamedChangelog.Line("### " + entry.heading(), by));
            lines.add(new BlamedChangelog.Line("- " + entry.text(), by));
        }
        return new BlamedChangelog(lines);
    }

    @Test
    void pendingIsWhatWasNotAnnounced() {
        News news = News.pending(Set.of(ADDED, FIXED), List.of(changelog(Contributor.Me.LOCAL, ADDED, BRAND_NEW, FIXED)));

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, BRAND_NEW))), news);
    }

    @Test
    void anEntryInSeveralChangelogsCountsOnceAsTheFirstChangelogHasIt() {
        List<BlamedChangelog> workingTreeThenUpstream = List.of(
                changelog(Contributor.Me.LOCAL, BRAND_NEW),
                changelog(Contributor.Me.REMOTE, BRAND_NEW));

        News news = News.pending(Set.of(), workingTreeThenUpstream);

        assertEquals(new News(List.of(new AttributedEntry(Contributor.Me.LOCAL, BRAND_NEW))), news);
    }

    @Test
    void allEntriesSpanEveryChangelogOnce() {
        List<BlamedChangelog> changelogs = List.of(changelog(ALICE, ADDED, FIXED), changelog(Contributor.Me.REMOTE, FIXED, BRAND_NEW));

        assertEquals(List.of(ADDED, FIXED, BRAND_NEW), List.copyOf(News.allEntries(changelogs)));
    }

    @Test
    void groupsListOthersAsTheyAppearThenMeRemotelyThenMe() {
        News news = new News(List.of(
                new AttributedEntry(Contributor.Me.LOCAL, ADDED),
                new AttributedEntry(BOB, FIXED),
                new AttributedEntry(Contributor.Me.REMOTE, BRAND_NEW),
                new AttributedEntry(ALICE, ADDED),
                new AttributedEntry(BOB, BRAND_NEW)));

        assertEquals(List.of(BOB, ALICE, Contributor.Me.REMOTE, Contributor.Me.LOCAL), List.copyOf(news.grouped().keySet()));
        assertEquals(List.of(new AttributedEntry(BOB, FIXED), new AttributedEntry(BOB, BRAND_NEW)), news.grouped().get(BOB));
    }

    @Test
    void plainTextIsOneBulletPerEntryUnderItsGroupTitle() {
        News news = new News(List.of(
                new AttributedEntry(Contributor.Me.LOCAL, new ChangelogEntry("Unreleased", "Added", "**Bold** start.")),
                new AttributedEntry(ALICE, ADDED)));

        assertEquals("""
                Changes by Alice
                • added

                Changes by me
                • Bold start.""", news.asPlainText());
    }

    @Test
    void newsHoldsItsItemsImmutably() {
        List<AttributedEntry> items = new ArrayList<>(List.of(new AttributedEntry(ALICE, ADDED)));
        News news = new News(items);
        items.clear();

        assertEquals(Map.of(ALICE, List.of(new AttributedEntry(ALICE, ADDED))), news.grouped());
    }
}
