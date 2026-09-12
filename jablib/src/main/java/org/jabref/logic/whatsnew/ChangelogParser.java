package org.jabref.logic.whatsnew;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

/// Reads the entries out of the lines of a `CHANGELOG.md` in the [keep a changelog](https://keepachangelog.com)
/// layout: `## [release] - date` sections holding `### Added/Changed/Fixed` headings holding `- ` entries.
@NullMarked
public final class ChangelogParser {

    private static final String SECTION_PREFIX = "## ";
    private static final String HEADING_PREFIX = "### ";
    private static final String ENTRY_PREFIX = "- ";

    /// `[6.0-alpha.6] - 2026-05-14` or `[Unreleased]`: the release and, optionally, its date.
    private static final Pattern SECTION_LABEL = Pattern.compile("^\\[(?<release>.*?)](?: - (?<date>.*))?$");

    private ChangelogParser() {
    }

    /// Every entry of `lines`, keyed by the index of its `- ` line, in the order of the file.
    /// Continuation lines of an entry are not part of the entry: JabRef's changelog keeps every entry on one line.
    public static SequencedMap<Integer, ChangelogEntry> entries(List<String> lines) {
        SequencedMap<Integer, ChangelogEntry> entries = new LinkedHashMap<>();
        String section = "";
        String heading = "";
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (line.startsWith(SECTION_PREFIX)) {
                section = sectionName(line.substring(SECTION_PREFIX.length()).strip());
            } else if (line.startsWith(HEADING_PREFIX)) {
                heading = line.substring(HEADING_PREFIX.length()).strip();
            } else if (line.startsWith(ENTRY_PREFIX)) {
                entries.put(lineIndex, new ChangelogEntry(section, heading, line.substring(ENTRY_PREFIX.length()).strip()));
            }
        }
        return entries;
    }

    /// `[6.0-alpha.6] - 2026-05-14` reads as `6.0-alpha.6 (2026-05-14)`, `[Unreleased]` as `Unreleased`; any other
    /// label stays as it is.
    private static String sectionName(String label) {
        Matcher matcher = SECTION_LABEL.matcher(label);
        if (!matcher.matches()) {
            return label;
        }
        String release = matcher.group("release");
        return matcher.group("date") == null ? release : release + " (" + matcher.group("date") + ")";
    }
}
