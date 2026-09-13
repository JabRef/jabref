package org.jabref.logic.whatsnew;

/// One `- ` line of `CHANGELOG.md` together with the `## ` release section and the `### ` heading it stands under.
/// Section and heading are for display; what makes an entry the same entry over time is its text, see
/// [News#pending].
///
/// @param section the release, e.g. `Unreleased` or `6.0-alpha.6 (2026-05-14)`
/// @param heading the kind of change, e.g. `Added`, `Changed` or `Fixed`
/// @param text    the entry itself, Markdown included, without the leading `- `
public record ChangelogEntry(String section, String heading, String text) {
}
