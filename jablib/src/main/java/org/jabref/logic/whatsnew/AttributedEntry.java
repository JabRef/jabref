package org.jabref.logic.whatsnew;

/// A changelog entry and who wrote it.
public record AttributedEntry(Contributor by, ChangelogEntry entry) {
}
