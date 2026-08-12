package org.jabref.logic.preview;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.Nullable;

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

    @Nullable
    static PreviewLayout of(String layout,
                            String customPreviewLayout,
                            List<Path> bstLayoutPaths,
                            LayoutFormatterPreferences preferences,
                            JournalAbbreviationRepository abbreviationRepository,
                            BibEntryTypesManager entryTypesManager) {
        if (CSLStyleUtils.isCitationStyleFile(layout)) {
            return CSLStyleUtils.createCitationStyleFromFile(layout)
                                .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file, entryTypesManager))
                                .orElse(null);
        }
        if (BstPreviewLayout.isBstStyleFile(layout)) {
            return bstLayoutPaths.stream()
                                 .filter(path -> path.endsWith(layout))
                                 .map(BstPreviewLayout::new)
                                 .findFirst()
                                 .orElse(null);
        } else if (TextBasedPreviewLayout.NAME.equals(layout)) {
            return TextBasedPreviewLayout.of(
                    customPreviewLayout,
                    preferences,
                    abbreviationRepository);
        }

        return null;
    }

    @Nullable
    static PreviewLayout of(String layoutIdentifier,
                            List<CustomizedPreviewStyle> customizedPreviewLayouts,
                            List<Path> bstLayoutPaths,
                            LayoutFormatterPreferences preferences,
                            JournalAbbreviationRepository abbreviationRepository,
                            BibEntryTypesManager entryTypesManager) {
        if (CSLStyleUtils.isCitationStyleFile(layoutIdentifier)) {
            return CSLStyleUtils.createCitationStyleFromFile(layoutIdentifier)
                                .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file, entryTypesManager))
                                .orElse(null);
        }
        if (BstPreviewLayout.isBstStyleFile(layoutIdentifier)) {
            return bstLayoutPaths.stream()
                                 .filter(path -> path.endsWith(layoutIdentifier))
                                 .map(BstPreviewLayout::new)
                                 .findFirst()
                                 .orElse(null);
        }

        // Text-based (customized) styles are resolved by stable id, not display name — names are user-editable.
        return customizedPreviewLayouts.stream()
                                       .filter(c -> c.id().equals(layoutIdentifier))
                                       .findFirst()
                                       .<PreviewLayout>map(c -> TextBasedPreviewLayout.of(
                                               c.id(), c.name(), c.text(), preferences, abbreviationRepository))
                                       .orElse(null);
    }
}
