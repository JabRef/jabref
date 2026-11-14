package org.jabref.logic.preview;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

/// Used for displaying a rendered entry in the UI. Due to historical reasons, "rendering" is called "layout".
public sealed interface PreviewLayout permits BstPreviewLayout, CitationStylePreviewLayout, TextBasedPreviewLayout {
    String generatePreview(BibEntry entry, BibDatabaseContext databaseContext);

    String getDisplayName();

    String getText();

    String getName();

    String getShortTitle();

    default boolean containsCaseIndependent(String searchTerm) {
        return this.getDisplayName().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT))
                || this.getShortTitle().toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }

    static Optional<PreviewLayout> of(String layoutIdentifier,
                                      List<CustomizedPreviewStyle> customizedPreviewLayouts,
                                      List<Path> bstLayoutPaths,
                                      LayoutFormatterPreferences preferences,
                                      JournalAbbreviationRepository abbreviationRepository,
                                      BibEntryTypesManager entryTypesManager) {
        if (CSLStyleUtils.isCitationStyleFile(layoutIdentifier)) {
            return CSLStyleUtils.createCitationStyleFromFile(layoutIdentifier)
                                .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file, entryTypesManager));
        }
        if (BstPreviewLayout.isBstStyleFile(layoutIdentifier)) {
            return bstLayoutPaths.stream()
                                 .filter(path -> path.endsWith(layoutIdentifier))
                                 .map(path -> (PreviewLayout) new BstPreviewLayout(path))
                                 .findFirst();
        }

        // Text-based (customized) styles are resolved by stable id, not display name — names are user-editable.
        return customizedPreviewLayouts.stream()
                                       .filter(c -> c.id().equals(layoutIdentifier))
                                       .findFirst()
                                       .map(c -> (PreviewLayout) TextBasedPreviewLayout.of(
                                               c.id(), c.name(), c.text(), preferences, abbreviationRepository));
    }
}
