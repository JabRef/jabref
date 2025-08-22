package org.jabref.logic.preview;

import java.util.Locale;

import org.jabref.logic.bst.BstPreviewLayout;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import jakarta.annotation.Nullable;

/**
 * Used for displaying a rendered entry in the UI. Due to historical reasons, "rendering" is called "layout".
 */
public sealed interface PreviewLayout permits BstPreviewLayout, CitationStylePreviewLayout, TextBasedPreviewLayout {

    String generatePreview(BibEntry entry, BibDatabaseContext databaseContext);

    String getDisplayName();

    String getText();

    String getName();

    @Nullable
    String getShortTitle();

    default boolean containsCaseIndependent(String searchTerm) {
        return this.getDisplayName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT))
                || (this.getShortTitle() != null && this.getShortTitle().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT)));
    }
}
