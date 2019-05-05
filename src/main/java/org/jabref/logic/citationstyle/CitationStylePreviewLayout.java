package org.jabref.logic.citationstyle;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CitationStylePreviewLayout implements PreviewLayout {
    private CitationStyle citationStyle;

    public CitationStylePreviewLayout(CitationStyle citationStyle) {
        this.citationStyle = citationStyle;
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabase database) {
        return CitationStyleGenerator.generateCitation(entry, citationStyle.getSource(), CitationStyleOutputFormat.HTML);
    }

    @Override
    public String getName() {
        return citationStyle.getTitle();
    }

    public String getSource() {
        return citationStyle.getSource();
    }

    public String getFilePath() {
        return citationStyle.getFilePath();
    }
}
