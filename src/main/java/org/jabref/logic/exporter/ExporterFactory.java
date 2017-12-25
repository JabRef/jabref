package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
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
        exporters.add(new TemplateExporter("HTML", "html", "html", null, FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("Simple HTML"), "simplehtml", "simplehtml", null, FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("DocBook 4.4", "docbook", "docbook", null, FileType.XML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("DIN 1505", "din1505", "din1505winword", "din1505", FileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("BibO RDF", "bibordf", "bibordf", null, FileType.RDF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML table"), "tablerefs", "tablerefs", "tablerefs", FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML list"), "listrefs", "listrefs", "listrefs", FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML table (with Abstract & BibTeX)"), "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", FileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("Harvard RTF", "harvard", "harvard", "harvard", FileType.RDF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("ISO 690 RTF", "iso690rtf", "iso690RTF", "iso690rtf", FileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("ISO 690", "iso690txt", "iso690", "iso690txt", FileType.TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("Endnote", "endnote", "EndNote", "endnote", FileType.TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("OpenOffice/LibreOffice CSV", "oocsv", "openoffice-csv", "openoffice", FileType.CSV, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("RIS", "ris", "ris", "ris", FileType.RIS, layoutPreferences, savePreferences).withEncoding(StandardCharsets.UTF_8));
        exporters.add(new TemplateExporter("MIS Quarterly", "misq", "misq", "misq", FileType.RTF, layoutPreferences, savePreferences));
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
        StringBuilder sb = new StringBuilder();
        int lastBreak = -firstLineSubtraction;

        for (Exporter exporter : exporters) {
            String name = exporter.getId();
            if (((sb.length() + 2 + name.length()) - lastBreak) > maxLineLength) {
                sb.append(",\n");
                lastBreak = sb.length();
                sb.append(linePrefix);
            } else if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }

        return sb.toString();
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
