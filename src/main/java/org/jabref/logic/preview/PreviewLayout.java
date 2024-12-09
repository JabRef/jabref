package org.jabref.logic.preview;

import java.util.Locale;

import org.jabref.logic.bst.BstPreviewLayout;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * Used for displaying a rendered entry in the UI. Due to historical reasons, "rendering" is called "layout".
 */
public sealed interface PreviewLayout permits BstPreviewLayout, CitationStylePreviewLayout, TextBasedPreviewLayout {

    String generatePreview(BibEntry entry, BibDatabaseContext databaseContext);

    String getDisplayName();

    String getText();

    String getName();

    default boolean containsCaseIndependent(String searchTerm) {
        return this.getDisplayName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }
}
