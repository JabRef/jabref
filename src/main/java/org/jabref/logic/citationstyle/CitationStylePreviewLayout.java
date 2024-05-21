package org.jabref.logic.citationstyle;

import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public final class CitationStylePreviewLayout implements PreviewLayout {
    private final CitationStyle citationStyle;
    private final BibEntryTypesManager bibEntryTypesManager;

    public CitationStylePreviewLayout(CitationStyle citationStyle, BibEntryTypesManager bibEntryTypesManager) {
        this.citationStyle = citationStyle;
        this.bibEntryTypesManager = bibEntryTypesManager;
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabaseContext databaseContext) {
        return CitationStyleGenerator.generateCitation(entry, citationStyle.getSource(), CitationStyleOutputFormat.HTML, databaseContext, bibEntryTypesManager);
    }

    @Override
    public String getDisplayName() {
        return citationStyle.getTitle();
    }

    @Override
    public String getText() {
        return citationStyle.getSource();
    }

    public String getFilePath() {
        return citationStyle.getFilePath();
    }

    @Override
    public String getName() {
        return citationStyle.getTitle();
    }
}
