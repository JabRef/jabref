package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.BasicFileType;
import org.jabref.logic.xmp.XmpPreferences;

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

    public static ExporterFactory create(Map<String, TemplateExporter> customFormats,
            LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences, XmpPreferences xmpPreferences) {

        List<Exporter> exporters = new ArrayList<>();

        // Initialize build-in exporters
        exporters.add(new TemplateExporter("html", "html", null, BasicFileType.HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("simplehtml", "simplehtml", null, BasicFileType.SIMPLE_HTML, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("docbook", "docbook", null, BasicFileType.DOCBOOK, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("din1505", "din1505winword", "din1505", BasicFileType.DIN_1505, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("bibordf", "bibordf", null, BasicFileType.BIBORDF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("tablerefs", "tablerefs", "tablerefs", BasicFileType.HTML_TABLE, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("listrefs", "listrefs", "listrefs", BasicFileType.HTML_LIST, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", BasicFileType.HTML_TABLE_WITH_ABSTRACT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("harvard", "harvard", "harvard", BasicFileType.HARVARD_RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("iso690rtf", "iso690RTF", "iso690rtf", BasicFileType.ISO_690_RTF, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("iso690txt", "iso690", "iso690txt", BasicFileType.ISO_690_TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("endnote", "EndNote", "endnote", BasicFileType.ENDNOTE_TXT, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("oocsv", "openoffice-csv", "openoffice", BasicFileType.OO_LO, layoutPreferences, savePreferences));
        exporters.add(new TemplateExporter("ris", "ris", "ris", BasicFileType.RIS, layoutPreferences, savePreferences, true).withEncoding(StandardCharsets.UTF_8));
        exporters.add(new TemplateExporter("misq", "misq", "misq", BasicFileType.MIS_QUARTERLY, layoutPreferences, savePreferences));
        exporters.add(new BibTeXMLExporter());
        exporters.add(new OpenOfficeDocumentCreator());
        exporters.add(new OpenDocumentSpreadsheetCreator());
        exporters.add(new MSBibExporter());
        exporters.add(new ModsExporter());
        exporters.add(new XmpExporter(xmpPreferences));
        exporters.add(new XmpPdfExporter(xmpPreferences));

        // Now add custom export formats
        exporters.addAll(customFormats.values());

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
