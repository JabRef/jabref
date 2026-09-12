package org.jabref.logic.whatsnew;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~whats-new.checkout-news~1]
class ChangelogParserTest {

    @Test
    void entriesKeepTheirSectionHeadingAndLine() {
        List<String> changelog = List.of(
                "## [Unreleased]",
                "",
                "### Added",
                "",
                "- We added a button. [#1](https://github.com/JabRef/jabref/pull/1)",
                "",
                "### Fixed",
                "",
                "- We fixed a crash.",
                "",
                "## [6.0-alpha.6] - 2026-05-14",
                "",
                "### Fixed",
                "",
                "- We fixed an older crash.");

        SequencedMap<Integer, ChangelogEntry> entries = ChangelogParser.entries(changelog);

        assertEquals(Map.of(
                4, new ChangelogEntry("Unreleased", "Added", "We added a button. [#1](https://github.com/JabRef/jabref/pull/1)"),
                8, new ChangelogEntry("Unreleased", "Fixed", "We fixed a crash."),
                14, new ChangelogEntry("6.0-alpha.6 (2026-05-14)", "Fixed", "We fixed an older crash.")), entries);
        assertEquals(List.of(4, 8, 14), List.copyOf(entries.keySet()));
    }

    @Test
    void aSectionWithoutBracketsIsTakenAsItIs() {
        SequencedMap<Integer, ChangelogEntry> entries = ChangelogParser.entries(List.of("## Next release", "- An entry."));

        assertEquals(Map.of(1, new ChangelogEntry("Next release", "", "An entry.")), entries);
    }

    @Test
    void textOutsideEntriesIsIgnored() {
        List<String> changelog = List.of(
                "# Changelog",
                "All notable changes are documented here.",
                "## [Unreleased]",
                "### Added",
                "- An entry.",
                "  continued on the next line.",
                "[Unreleased]: https://github.com/JabRef/jabref/compare/v6.0...HEAD");

        assertEquals(Map.of(4, new ChangelogEntry("Unreleased", "Added", "An entry.")), ChangelogParser.entries(changelog));
    }
}
