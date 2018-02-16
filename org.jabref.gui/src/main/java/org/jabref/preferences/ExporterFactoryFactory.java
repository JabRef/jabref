package org.jabref.preferences;

import java.util.Map;

import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.layout.LayoutFormatterPreferences;

public class ExporterFactoryFactory {

    public static ExporterFactory create(JabRefPreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        Map<String, TemplateExporter> customFormats = preferences.customExports.getCustomExportFormats(preferences, abbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(abbreviationLoader);
        SavePreferences savePreferences = JabRefPreferences.loadForExportFromPreferences(preferences);
        return ExporterFactory.create(customFormats, layoutPreferences, savePreferences);
    }

}
