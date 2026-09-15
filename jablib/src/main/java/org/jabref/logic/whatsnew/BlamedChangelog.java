package org.jabref.logic.whatsnew;

import java.util.List;

/// A `CHANGELOG.md` as `git blame` sees it: every line with the contributor who wrote it.
public record BlamedChangelog(List<Line> lines) {
    /// One line and who wrote it.
    public record Line(String text, Contributor by) {
    }

    public BlamedChangelog {
        lines = List.copyOf(lines);
    }

    /// The entries of the changelog, each attributed to the contributor of its `- ` line.
    public List<AttributedEntry> entries() {
        return ChangelogParser.entries(lines.stream().map(Line::text).toList())
                              .entrySet().stream()
                              .map(entry -> new AttributedEntry(lines.get(entry.getKey()).by(), entry.getValue()))
                              .toList();
    }
}
