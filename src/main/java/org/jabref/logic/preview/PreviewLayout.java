package org.jabref.logic.preview;

import java.util.Locale;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

/**
 * Used for displaying a rendered entry in the UI. Due to historical reasons, "rendering" is called "layout".
 */
public interface PreviewLayout {

    String generatePreview(BibEntry entry, BibDatabase database);

    String getDisplayName();

    String getName();

    default boolean containsCaseIndependent(String searchTerm) {
        return this.getDisplayName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }
}
