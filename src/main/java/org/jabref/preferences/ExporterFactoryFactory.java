package org.jabref.preferences;

import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.layout.LayoutFormatterPreferences;

import java.util.Map;

public class ExporterFactoryFactory {

    public static ExporterFactory create(JabRefPreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        Map<String, TemplateExporter> customFormats = preferences.customExports.getCustomExportFormats(preferences, abbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(abbreviationLoader);
        SavePreferences savePreferences = SavePreferencesFactory.loadForExportFromPreferences(preferences);
        return ExporterFactory.create(customFormats, layoutPreferences, savePreferences);
    }

}
