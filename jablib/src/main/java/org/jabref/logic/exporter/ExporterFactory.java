package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.jspecify.annotations.NonNull;

public class ExporterFactory {

    private final List<Exporter> exporters;

    private ExporterFactory(@NonNull List<Exporter> exporters) {
        this.exporters = exporters;
    }

    public static ExporterFactory create(CliPreferences preferences) {
        List<TemplateExporter> customFormats = preferences.getExportPreferences().getCustomExporters();
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences();
        SelfContainedSaveOrder saveOrder = SelfContainedSaveOrder.of(preferences.getSelfContainedExportConfiguration().getSaveOrder());
        XmpPreferences xmpPreferences = preferences.getXmpPreferences();
        FieldPreferences fieldPreferences = preferences.getFieldPreferences();
        BibDatabaseMode bibDatabaseMode = preferences.getLibraryPreferences().getDefaultBibDatabaseMode();

        List<Exporter> exporters = new ArrayList<>();

        // Initialize build-in exporters
        exporters.add(new TemplateExporter("HTML", "html", "html", null, StandardFileType.HTML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter(Localization.lang("Simple HTML"), "simplehtml", "simplehtml", null, StandardFileType.HTML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("DocBook 5.1", "docbook5", "docbook5", null, StandardFileType.XML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("DocBook 4", "docbook4", "docbook4", null, StandardFileType.XML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("DIN 1505", "din1505", "din1505winword", "din1505", StandardFileType.RTF, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("BibO RDF", "bibordf", "bibordf", null, StandardFileType.RDF, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter(Localization.lang("HTML table"), "tablerefs", "tablerefs", "tablerefs", StandardFileType.HTML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter(Localization.lang("HTML list"), "listrefs", "listrefs", "listrefs", StandardFileType.HTML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter(Localization.lang("HTML table (with Abstract & BibTeX)"), "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", StandardFileType.HTML, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter(Localization.lang("Markdown titles"), "title-md", "title-md", "title-markdown", StandardFileType.MARKDOWN, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("Harvard RTF", "harvard", "harvard", "harvard", StandardFileType.RTF, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("ISO 690 RTF", "iso690rtf", "iso690RTF", "iso690rtf", StandardFileType.RTF, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("ISO 690", "iso690txt", "iso690", "iso690txt", StandardFileType.TXT, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("Endnote", "endnote", "EndNote", "endnote", StandardFileType.TXT, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("OpenOffice/LibreOffice CSV", "oocsv", "openoffice-csv", "openoffice", StandardFileType.CSV, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("RIS", "ris", "ris", "ris", StandardFileType.RIS, layoutPreferences, saveOrder, BlankLineBehaviour.DELETE_BLANKS));
        exporters.add(new TemplateExporter("MIS Quarterly", "misq", "misq", "misq", StandardFileType.RTF, layoutPreferences, saveOrder));
        exporters.add(new TemplateExporter("CSL YAML", "yaml", "yaml", null, StandardFileType.YAML, layoutPreferences, saveOrder, BlankLineBehaviour.DELETE_BLANKS));
        exporters.add(new TemplateExporter("Hayagriva YAML", "hayagrivayaml", "hayagrivayaml", null, StandardFileType.YAML, layoutPreferences, saveOrder, BlankLineBehaviour.DELETE_BLANKS));
        exporters.add(new OpenOfficeDocumentCreator());
        exporters.add(new OpenDocumentSpreadsheetCreator());
        exporters.add(new MSBibExporter());
        exporters.add(new ModsExporter());
        exporters.add(new XmpExporter(xmpPreferences));
        exporters.add(new XmpPdfExporter(xmpPreferences));
        exporters.add(new EmbeddedBibFilePdfExporter(bibDatabaseMode, preferences.getCustomEntryTypesRepository(), fieldPreferences));
        exporters.add(new CffExporter());
        exporters.add(new EndnoteXmlExporter(preferences.getBibEntryPreferences()));

        // Now add custom export formats
        exporters.addAll(customFormats);

        return new ExporterFactory(exporters);
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
