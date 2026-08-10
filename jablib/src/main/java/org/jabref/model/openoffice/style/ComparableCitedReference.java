package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// This is what we need to sort bibliography entries.
@NullMarked
public interface ComparableCitedReference {

    String getCitationKey();

    Optional<BibEntry> getBibEntry();
}
