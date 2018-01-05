package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.FileType;
import org.jabref.preferences.JabRefPreferences;

public class ExporterFactory {

    /**
     * Global variable that is used for counting output entries when exporting:
     *
     * @deprecated find a better way to do this
     */
    @Deprecated
    public static int entryNumber;

    private final List<Exporter> exporters;

    private ExporterFactory(List<Exporter> exporters) {
        this.exporters = Objects.requireNonNull(exporters);
    }

    public static ExporterFactory create(Map<String, TemplateExporter> customFormats,
                                         LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {

        List<Exporter> exporters = new ArrayList<>();

        // Initialize build-in exporters
        exporters.add(new TemplateExporter(FileType.HTML.getDescription(), "html", "html", null, FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.SIMPLE_HTML.getDescription(), "simplehtml", "simplehtml", null, FileType.SIMPLE_HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.DOCBOOK.getDescription(), "docbook", "docbook", null, FileType.DOCBOOK, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.DIN_1505.getDescription(), "din1505", "din1505winword", "din1505", FileType.DIN_1505, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.BIBORDF.getDescription(), "bibordf", "bibordf", null, FileType.BIBORDF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.HTML_TABLE.getDescription(), "tablerefs", "tablerefs", "tablerefs", FileType.HTML_TABLE, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.HTML_LIST.getDescription(), "listrefs", "listrefs", "listrefs", FileType.HTML_LIST, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.HTML_TABLE_WITH_ABSTRACT.getDescription(), "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", FileType.HTML_TABLE_WITH_ABSTRACT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.HARVARD_RTF.getDescription(), "harvard", "harvard", "harvard", FileType.HARVARD_RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.ISO_690_RTF.getDescription(), "iso690rtf", "iso690RTF", "iso690rtf", FileType.ISO_690_RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.ISO_690_TXT.getDescription(), "iso690txt", "iso690", "iso690txt", FileType.ISO_690_TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.ENDNOTE_TXT.getDescription(), "endnote", "EndNote", "endnote", FileType.ENDNOTE_TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.OO_LO.getDescription(), "oocsv", "openoffice-csv", "openoffice", FileType.OO_LO, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(FileType.RIS.getDescription(), "ris", "ris", "ris", FileType.RIS, layoutPreferences, savePreferences).withEncoding(StandardCharsets.UTF_8));
        exporters.add(new TemplateExporter(FileType.MIS_QUARTERLY.getDescription(), "misq", "misq", "misq", FileType.MIS_QUARTERLY, layoutPreferences, savePreferences));
        exporters.add(new BibTeXMLExporter());
        exporters.add(new OpenOfficeDocumentCreator());
        exporters.add(new OpenDocumentSpreadsheetCreator());
        exporters.add(new MSBibExporter());
        exporters.add(new ModsExporter());

        // Now add custom export formats
        exporters.addAll(customFormats.values());

        return new ExporterFactory(exporters);
    }

    public static ExporterFactory create(JabRefPreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        Map<String, TemplateExporter> customFormats = preferences.customExports.getCustomExportFormats(preferences, abbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(abbreviationLoader);
        SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(preferences);
        return create(customFormats, layoutPreferences, savePreferences);
    }

    /**
     * Build a string listing of all available exporters.
     *
     * @param maxLineLength The max line length before a line break must be added.
     * @param linePrefix    If a line break is added, this prefix will be inserted at the beginning of the next line.
     * @return The string describing available exporters.
     */
    public String getExportersAsString(int maxLineLength, int firstLineSubtraction, String linePrefix) {
        StringBuilder builder = new StringBuilder();
        int lastBreak = -firstLineSubtraction;

        for (Exporter exporter : exporters) {
            String name = exporter.getId();
            if (((builder.length() + 2 + name.length()) - lastBreak) > maxLineLength) {
                builder.append(",\n");
                lastBreak = builder.length();
                builder.append(linePrefix);
            } else if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
        }

        return builder.toString();
    }

    /**
     * Get a list of all exporters.
     *
     * @return A list containing all exporters
     */
    public List<Exporter> getExporters() {
        return Collections.unmodifiableList(exporters);
    }

    /**
     * Look up the named exporter (case-insensitive).
     *
     * @param consoleName The export name given in the JabRef console help information.
     * @return The exporter, or an empty option if no exporter with that name is registered.
     */
    public Optional<Exporter> getExporterByName(String consoleName) {
        return exporters.stream().filter(exporter -> exporter.getId().equalsIgnoreCase(consoleName)).findFirst();
    }
}
