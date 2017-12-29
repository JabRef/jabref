package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.FileExtensions;

public class ExportFormats {

    // Global variable that is used for counting output entries when exporting:
    public static int entryNumber;

    private static final Map<String, IExportFormat> EXPORT_FORMATS = new TreeMap<>();

    private ExportFormats() {
    }

    public static void initAllExports(Map<String, ExportFormat> customFormats,
                                      LayoutFormatterPreferences layoutPreferences, SavePreferences savePreferences) {

        ExportFormats.EXPORT_FORMATS.clear();

        // Initialize Build-In Export Formats
        ExportFormats
                .putFormat(new ExportFormat("HTML", "html", "html", null, FileExtensions.HTML, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("Simple HTML"), "simplehtml", "simplehtml", null,
                FileExtensions.HTML, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat("DocBook 4.4", "docbook", "docbook", null, FileExtensions.XML, layoutPreferences,
                savePreferences));
        ExportFormats.putFormat(new ExportFormat("DIN 1505", "din1505", "din1505winword", "din1505", FileExtensions.RTF,
                layoutPreferences, savePreferences));
        ExportFormats.putFormat(
                new ExportFormat("BibO RDF", "bibordf", "bibordf", null, FileExtensions.RDF, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML table"), "tablerefs", "tablerefs", "tablerefs",
                FileExtensions.HTML, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML list"), "listrefs", "listrefs", "listrefs",
                FileExtensions.HTML, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat(Localization.lang("HTML table (with Abstract & BibTeX)"),
                "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", FileExtensions.HTML, layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat("Harvard RTF", "harvard", "harvard", "harvard", FileExtensions.RDF,
                layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat("ISO 690 RTF", "iso690rtf", "iso690RTF", "iso690rtf", FileExtensions.RTF,
                layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat("ISO 690", "iso690txt", "iso690", "iso690txt", FileExtensions.TXT,
                layoutPreferences, savePreferences));
        ExportFormats.putFormat(new ExportFormat("Endnote", "endnote", "EndNote", "endnote", FileExtensions.TXT, layoutPreferences,
                savePreferences));
        ExportFormats.putFormat(new ExportFormat("OpenOffice/LibreOffice CSV", "oocsv", "openoffice-csv", "openoffice",
                FileExtensions.CSV, layoutPreferences, savePreferences));
        ExportFormat ef = new ExportFormat("RIS", "ris", "ris", "ris", FileExtensions.RIS, layoutPreferences, savePreferences);
        ef.setEncoding(StandardCharsets.UTF_8);
        ExportFormats.putFormat(ef);
        ExportFormats.putFormat(
                new ExportFormat("MIS Quarterly", "misq", "misq", "misq", FileExtensions.RTF, layoutPreferences, savePreferences));

        ExportFormats.putFormat(new BibTeXMLExportFormat());
        ExportFormats.putFormat(new OpenOfficeDocumentCreator());
        ExportFormats.putFormat(new OpenDocumentSpreadsheetCreator());
        ExportFormats.putFormat(new MSBibExportFormat());
        ExportFormats.putFormat(new ModsExportFormat());

        // Now add custom export formats
        for (IExportFormat format : customFormats.values()) {
            ExportFormats.putFormat(format);
        }
    }

    /**
     * Build a string listing of all available export formats.
     *
     * @param maxLineLength The max line length before a line break must be added.
     * @param linePrefix    If a line break is added, this prefix will be inserted at the
     *                      beginning of the next line.
     * @return The string describing available formats.
     */
    public static String getConsoleExportList(int maxLineLength, int firstLineSubtr, String linePrefix) {
        StringBuilder sb = new StringBuilder();
        int lastBreak = -firstLineSubtr;

        for (String name : ExportFormats.EXPORT_FORMATS.keySet()) {
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
     * Get a Map of all export formats.
     *
     * @return A Map containing all export formats, mapped to their console names.
     */
    public static Map<String, IExportFormat> getExportFormats() {
        // It is perhaps overly paranoid to make a defensive copy in this case:
        return Collections.unmodifiableMap(ExportFormats.EXPORT_FORMATS);
    }

    /**
     * Look up the named export format.
     *
     * @param consoleName The export name given in the JabRef console help information.
     * @return The ExportFormat, or null if no exportformat with that name is
     * registered.
     */
    public static IExportFormat getExportFormat(String consoleName) {
        return ExportFormats.EXPORT_FORMATS.get(consoleName);
    }

    public static FileExtensions getFileExtension(String consoleName) {
        if (checkExportFormatExisit(consoleName)) {
            ExportFormat exportFormat = (ExportFormat) EXPORT_FORMATS.get(consoleName);
            return exportFormat.getExtension();
        } else {
            return FileExtensions.DEFAULT;
        }
    }

    private static boolean checkExportFormatExisit(String consoleName) {
        return EXPORT_FORMATS.keySet().contains(consoleName);
    }

    private static void putFormat(IExportFormat format) {
        ExportFormats.EXPORT_FORMATS.put(format.getConsoleName(), format);
    }
}
