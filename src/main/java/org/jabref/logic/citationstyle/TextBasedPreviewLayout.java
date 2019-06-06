package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.StringReader;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextBasedPreviewLayout implements PreviewLayout {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextBasedPreviewLayout.class);

    private Layout layout;

    public TextBasedPreviewLayout(String layoutText, LayoutFormatterPreferences layoutFormatterPreferences) {
        StringReader sr = new StringReader(layoutText.replace("__NEWLINE__", "\n"));
        try {
            layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.error("Could not generate layout", e);
        }
    }

    public TextBasedPreviewLayout(Layout layout) {
        this.layout = layout;
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabase database) {
        if (layout != null) {
            return layout.doLayout(entry, database);
        } else {
            return "";
        }
    }

    @Override
    public String getName() {
        return Localization.lang("Preview");
    }
}
