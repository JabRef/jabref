package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

/**
 * This is what we need to sort bibliography entries.
 */
public interface ComparableCitedKey {

    String getCitationKey();

    Optional<BibEntry> getBibEntry();
}

