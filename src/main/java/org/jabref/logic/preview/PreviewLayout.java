package org.jabref.logic.preview;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

/**
 * Used for displaying a rendered entry in the UI. Due to historical reasons, "rendering" is called "layout".
 */
public interface PreviewLayout {

    String generatePreview(BibEntry entry, BibDatabase database);

    String getDisplayName();

    String getName();
}
