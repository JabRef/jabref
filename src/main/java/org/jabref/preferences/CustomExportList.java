package org.jabref.preferences;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.FileType;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles user defined custom export formats. They are initially
 * read from Preferences, and kept alphabetically (sorted by name). Formats can
 * be added or removed. When modified, the sort() method must be called to make
 * sure the formats stay properly sorted. When the method store() is called,
 * export formats are written to Preferences.
 */

public class CustomExportList {

    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomExportList.class);
    private final EventList<List<String>> list;
    private final SortedList<List<String>> sorted;

    private final Map<String, TemplateExporter> formats = new TreeMap<>();

    public CustomExportList(Comparator<List<String>> comp) {
        list = new BasicEventList<>();
        sorted = new SortedList<>(list, comp);
    }

    public int size() {
        return list.size();
    }

    public EventList<List<String>> getSortedList() {
        return sorted;
    }

    public Map<String, TemplateExporter> getCustomExportFormats(JabRefPreferences prefs,
            JournalAbbreviationLoader loader) {
        Objects.requireNonNull(prefs);
        Objects.requireNonNull(loader);
        formats.clear();
        readPrefs(prefs, loader);
        return formats;
    }

    private void readPrefs(JabRefPreferences prefs, JournalAbbreviationLoader loader) {
        Objects.requireNonNull(prefs);
        Objects.requireNonNull(loader);
        formats.clear();
        list.clear();
        int i = 0;
        List<String> s;
        LayoutFormatterPreferences layoutPreferences = prefs.getLayoutFormatterPreferences(loader);
        SavePreferences savePreferences = prefs.loadForExportFromPreferences();
        while (!((s = prefs.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i)).isEmpty())) {
            Optional<TemplateExporter> format = createFormat(s.get(EXPORTER_NAME_INDEX), s.get(EXPORTER_FILENAME_INDEX), s.get(EXPORTER_EXTENSION_INDEX), layoutPreferences, savePreferences);
            if (format.isPresent()) {
                formats.put(format.get().getId(), format.get());
                list.add(s);
            } else {
                String customExportFormat = prefs.get(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
                LOGGER.error("Error initializing custom export format from string " + customExportFormat);
            }
            i++;
        }
    }

    private Optional<TemplateExporter> createFormat(String exporterName, String filename, String extension, LayoutFormatterPreferences layoutPreferences,
            SavePreferences savePreferences) {

        String lfFileName;
        if (extension.endsWith(".layout")) {
            lfFileName = filename.substring(0, filename.length() - ".layout".length());
        } else {
            lfFileName = filename;
        }
        TemplateExporter format = new TemplateExporter(exporterName, filename, lfFileName, null, FileType.parse(extension), layoutPreferences,
                savePreferences);
        format.setCustomExport(true);
        return Optional.of(format);
    }

    public void store(JabRefPreferences prefs) {

        if (list.isEmpty()) {
            purge(0, prefs);
        } else {
            for (int i = 0; i < list.size(); i++) {
                prefs.putStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i, list.get(i));
            }
            purge(list.size(), prefs);
        }
    }

    private void purge(int from, JabRefPreferences prefs) {
        int i = from;
        while (!prefs.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i).isEmpty()) {
            prefs.remove(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
            i++;
        }
    }

    public void remove(List<String> toRemove, LayoutFormatterPreferences layoutPreferences,
            SavePreferences savePreferences) {
        createFormat(toRemove.get(EXPORTER_NAME_INDEX), toRemove.get(EXPORTER_FILENAME_INDEX), toRemove.get(EXPORTER_EXTENSION_INDEX), layoutPreferences, savePreferences).ifPresent(format -> {
            formats.remove(format.getId());
            list.remove(toRemove);
        });
    }

    public void addFormat(String name, String layoutFile, String extension, LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {
        createFormat(name, layoutFile, extension, layoutPreferences, savePreferences).ifPresent(format -> {
            formats.put(format.getId(), format);
            list.add(Arrays.asList(name, layoutFile, extension));
        });

    }
}
