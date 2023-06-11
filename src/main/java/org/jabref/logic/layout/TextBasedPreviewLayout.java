package org.jabref.logic.layout;

import java.io.IOException;
import java.io.StringReader;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the preview based JabRef's <a href="https://docs.jabref.org/import-export/export/customexports">Custom export filters</a>.
 */
public class TextBasedPreviewLayout implements PreviewLayout {
    public static final String NAME = "PREVIEW";

    private static final Logger LOGGER = LoggerFactory.getLogger(TextBasedPreviewLayout.class);
    private Layout layout;
    private String text;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private JournalAbbreviationRepository abbreviationRepository;

    public TextBasedPreviewLayout(String text, LayoutFormatterPreferences layoutFormatterPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.abbreviationRepository = abbreviationRepository;
        setText(text);
    }

    public TextBasedPreviewLayout(Layout layout) {
        this.layout = layout;
        this.text = layout.getText();
    }

    public void setText(String text) {
        this.text = text;
        StringReader sr = new StringReader(text.replace("__NEWLINE__", "\n"));
        try {
            layout = new LayoutHelper(sr, layoutFormatterPreferences, abbreviationRepository).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.error("Could not generate layout", e);
        }
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabaseContext databaseContext) {
        if (layout != null) {
            return layout.doLayout(entry, databaseContext.getDatabase());
        } else {
            return "";
        }
    }

    public String getText() {
        return text;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return Localization.lang("Customized preview style");
    }
}
