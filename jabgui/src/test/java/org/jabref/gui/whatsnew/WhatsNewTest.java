package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~whats-new.checkout-news~1]
class WhatsNewTest {

    private static final String ME_SHA = "1111111111111111111111111111111111111111";
    private static final String OTHER_SHA = "2222222222222222222222222222222222222222";
    private static final String UNCOMMITTED_SHA = "0000000000000000000000000000000000000000";

    private static List<String> porcelain(String sha, String author, String mail, String text) {
        return List.of(
                sha + " 1 1 1",
                "author " + author,
                "author-mail <" + mail + ">",
                "\t" + text);
    }

    private static List<String> changelog(String... entries) {
        return List.of(
                "## [Unreleased]",
                "",
                "### Added",
                "",
                "- " + entries[0],
                "",
                "### Fixed",
                "",
                "- " + entries[1],
                "",
                "## [6.0-alpha.6] - 2026-05-14",
                "",
                "### Fixed",
                "",
                "- " + entries[2]);
    }

    @Test
    void parseAttributesLinesToMeByMailAndUncommitted() {
        List<String> porcelain = new ArrayList<>();
        porcelain.addAll(porcelain(ME_SHA, "Me Myself", "me@example.org", "- mine"));
        porcelain.addAll(porcelain(OTHER_SHA, "Other Author", "other@example.org", "- theirs"));
        porcelain.addAll(porcelain(UNCOMMITTED_SHA, "Not Committed Yet", "not.committed.yet", "- local edit"));

        WhatsNew.Source source = WhatsNew.parse(porcelain, "ME@example.org", WhatsNew.ME);

        assertEquals(List.of("- mine", "- theirs", "- local edit"), source.lines());
        assertEquals(List.of(WhatsNew.ME, "Other Author", WhatsNew.ME), source.by());
    }

    @Test
    void entriesKeepSectionAndHeading() {
        List<WhatsNew.Item> items = WhatsNew.entries(changelog("added", "fixed", "old fix"), _ -> Optional.of("Other Author"));

        assertEquals(List.of(
                new WhatsNew.Item("Other Author", "Unreleased", "Added", "added"),
                new WhatsNew.Item("Other Author", "Unreleased", "Fixed", "fixed"),
                new WhatsNew.Item("Other Author", "6.0-alpha.6 (2026-05-14)", "Fixed", "old fix")), items);
    }

    @Test
    void pendingIsWhatTheAnnouncedCopyLacks(@TempDir Path dir) throws IOException {
        Path copy = dir.resolve("announced.md");
        Files.write(copy, changelog("added", "fixed", "old fix"));
        List<String> now = changelog("added", "fixed", "old fix");
        List<String> lines = new ArrayList<>(now);
        lines.add(4, "- brand new");
        List<String> by = new ArrayList<>(Collections.nCopies(lines.size(), "Other Author"));
        by.set(4, WhatsNew.ME);

        List<WhatsNew.Item> pending = WhatsNew.pending(copy, List.of(new WhatsNew.Source(lines, by)));

        assertEquals(List.of(new WhatsNew.Item(WhatsNew.ME, "Unreleased", "Added", "brand new")), pending);
    }

    @Test
    void pendingIsEmptyWithoutAnnouncedCopy(@TempDir Path dir) {
        List<String> lines = changelog("added", "fixed", "old fix");
        List<String> by = Collections.nCopies(lines.size(), "Other Author");

        assertEquals(List.of(), WhatsNew.pending(dir.resolve("missing.md"), List.of(new WhatsNew.Source(lines, by))));
    }

    @Test
    void anEntryInBothSourcesCountsOnceAndLocalWins(@TempDir Path dir) throws IOException {
        Path copy = dir.resolve("announced.md");
        Files.write(copy, changelog("added", "fixed", "old fix"));
        List<String> lines = new ArrayList<>(changelog("added", "fixed", "old fix"));
        lines.add(4, "- pulled");
        List<String> local = new ArrayList<>(Collections.nCopies(lines.size(), "Other Author"));
        local.set(4, WhatsNew.ME);
        List<String> upstream = new ArrayList<>(Collections.nCopies(lines.size(), "Other Author"));
        upstream.set(4, WhatsNew.ME_REMOTELY);

        List<WhatsNew.Item> pending = WhatsNew.pending(copy,
                List.of(new WhatsNew.Source(lines, local), new WhatsNew.Source(lines, upstream)));

        assertEquals(List.of(new WhatsNew.Item(WhatsNew.ME, "Unreleased", "Added", "pulled")), pending);
    }

    @Test
    void groupsListOthersFirstThenMeRemotelyThenMe() {
        List<WhatsNew.Item> items = List.of(
                new WhatsNew.Item(WhatsNew.ME, "Unreleased", "Added", "a"),
                new WhatsNew.Item("Bob", "Unreleased", "Added", "b"),
                new WhatsNew.Item(WhatsNew.ME_REMOTELY, "Unreleased", "Added", "c"),
                new WhatsNew.Item("Alice", "Unreleased", "Added", "d"),
                new WhatsNew.Item("Bob", "Unreleased", "Fixed", "e"));

        assertEquals(List.of("Bob", "Alice", WhatsNew.ME_REMOTELY, WhatsNew.ME), WhatsNew.groups(items));
    }
}
