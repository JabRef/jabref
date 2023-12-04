package org.jabref.logic.journals.predatory;

import java.io.Serializable;

/**
 * Represents predatory journal information
 *
 * @param name The full journal name
 * @param abbr Abbreviation, if any
 * @param url  Url of the journal
 */
public record PredatoryJournalInformation(
        String name,
        String abbr,
        String url) implements Serializable {  // must implement @Serializable otherwise MVStore fails
}
