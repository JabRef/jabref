package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;

public class ExporterFactory {

    /**
     * Global variable that is used for counting output entries when exporting:
     *
     * @deprecated find a better way to do this
     */
    @Deprecated public static int entryNumber;

    private final List<Exporter> exporters;

    private ExporterFactory(List<Exporter> exporters) {
        this.exporters = Objects.requireNonNull(exporters);
    }

    public static ExporterFactory create(List<TemplateExporter> customFormats,
                                         LayoutFormatterPreferences layoutPreferences,
                                         SavePreferences savePreferences,
                                         XmpPreferences xmpPreferences,
                                         BibDatabaseMode bibDatabaseMode,
                                         BibEntryTypesManager entryTypesManager) {

        List<Exporter> exporters = new ArrayList<>();

        // Initialize build-in exporters

        // Initialize build-in exporters
        exporters.add(new TemplateExporter("HTML", "html", "html", null, StandardFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("Simple HTML"), "simplehtml", "simplehtml", null, StandardFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("DocBook 5.1", "docbook5", "docbook5", null, StandardFileType.XML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("DocBook 4", "docbook4", "docbook4", null, StandardFileType.XML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("DIN 1505", "din1505", "din1505winword", "din1505", StandardFileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("BibO RDF", "bibordf", "bibordf", null, StandardFileType.RDF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML table"), "tablerefs", "tablerefs", "tablerefs", StandardFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML list"), "listrefs", "listrefs", "listrefs", StandardFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter(Localization.lang("HTML table (with Abstract & BibTeX)"), "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", StandardFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("Harvard RTF", "harvard", "harvard", "harvard", StandardFileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("ISO 690 RTF", "iso690rtf", "iso690RTF", "iso690rtf", StandardFileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("ISO 690", "iso690txt", "iso690", "iso690txt", StandardFileType.TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("Endnote", "endnote", "EndNote", "endnote", StandardFileType.TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("OpenOffice/LibreOffice CSV", "oocsv", "openoffice-csv", "openoffice", StandardFileType.CSV, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("RIS", "ris", "ris", "ris", StandardFileType.RIS, layoutPreferences, savePreferences, BlankLineBehaviour.DELETE_BLANKS).withEncoding(StandardCharsets.UTF_8));
        exporters.add(new TemplateExporter("MIS Quarterly", "misq", "misq", "misq", StandardFileType.RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("CSL YAML", "yaml", "yaml", null, StandardFileType.YAML, layoutPreferences, savePreferences, BlankLineBehaviour.DELETE_BLANKS));
        exporters.add(new BibTeXMLExporter());
        exporters.add(new OpenOfficeDocumentCreator());
        exporters.add(new OpenDocumentSpreadsheetCreator());
        exporters.add(new MSBibExporter());
        exporters.add(new ModsExporter());
        exporters.add(new XmpExporter(xmpPreferences));
        exporters.add(new XmpPdfExporter(xmpPreferences));
        exporters.add(new EmbeddedBibFilePdfExporter(bibDatabaseMode, entryTypesManager, savePreferences.getFieldWriterPreferences()));

        // Now add custom export formats
        exporters.addAll(customFormats);

        return new ExporterFactory(exporters);
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
