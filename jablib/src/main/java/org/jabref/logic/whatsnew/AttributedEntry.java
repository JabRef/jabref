package org.jabref.logic.whatsnew;

import org.jspecify.annotations.NullMarked;

/// A changelog entry and who wrote it.
@NullMarked
public record AttributedEntry(Contributor by, ChangelogEntry entry) {
}
