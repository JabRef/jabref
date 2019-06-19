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
    private String layoutText;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    public TextBasedPreviewLayout(String layoutText, LayoutFormatterPreferences layoutFormatterPreferences) {
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        setLayoutText(layoutText);
    }

    public TextBasedPreviewLayout(Layout layout) {
        this.layout = layout;
        this.layoutText = layout.toString();
    }

    public void setLayoutText(String layoutText) {
        this.layoutText = layoutText;
        StringReader sr = new StringReader(layoutText.replace("__NEWLINE__", "\n"));
        try {
            layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.error("Could not generate layout", e);
        }
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabase database) {
        if (layout != null) {
            return layout.doLayout(entry, database);
        } else {
            return "";
        }
    }

    public String getLayoutText() {
        return layoutText;
    }

    @Override
    public String getName() {
        return Localization.lang("Preview");
    }
}
