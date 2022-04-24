package org.jabref.logic.layout.format;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Implements the preview based JabRef's <a href="https://docs.jabref.org/import-export/export/customexports">Custom export fitlters</a>.
 */
public class LaTex2UnicodePreviewLayout extends TextBasedPreviewLayout implements PreviewLayout {
    public static final String NAME = "Unicode";

    private static final Logger LOGGER = LoggerFactory.getLogger(LaTex2UnicodePreviewLayout.class);
    private Layout layout;
    private String text;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    public LaTex2UnicodePreviewLayout(String text, LayoutFormatterPreferences layoutFormatterPreferences) {
        super(text, layoutFormatterPreferences);
    }

    public LaTex2UnicodePreviewLayout(Layout layout) {
        super(layout);
    }

    public void setText(String text) {
        this.text = text;
        StringReader sr = new StringReader(text.replace("__NEWLINE__", "\n"));
        try {
            layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.error("Could not generate layout", e);
        }
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabaseContext databaseContext) {
        if (layout != null) {
            LatexToUnicodeFormatter latexToUnicodeFormatter=new LatexToUnicodeFormatter();
            return latexToUnicodeFormatter.format(layout.doLayout(entry, databaseContext.getDatabase()));
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
        return Localization.lang("Unicode");
    }
}
