package org.jabref.logic.citationstyle;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public record CitationStylePreviewLayout(
        CitationStyle citationStyle,
        BibEntryTypesManager bibEntryTypesManager) implements PreviewLayout {

    @Override
    public String generatePreview(List<BibEntry> entries, BibDatabaseContext databaseContext) {
        if (!citationStyle.hasBibliography()) {
            // style has no bibliography formatting instructions - fall back to citation
            return CitationStyleGenerator.generateCitation(entries, citationStyle.getSource(), CitationStyleOutputFormat.HTML, databaseContext, bibEntryTypesManager);
        }
        return CitationStyleGenerator.generateBibliography(entries, citationStyle.getSource(), CitationStyleOutputFormat.HTML, databaseContext, bibEntryTypesManager).stream().collect(Collectors.joining());
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

    @Override
    public String getShortTitle() {
        return citationStyle.getShortTitle();
    }
}
