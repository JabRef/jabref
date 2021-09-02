package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

public class ExternalFilesContentImporter {

    private final GeneralPreferences generalPreferences;
    private final ImportSettingsPreferences importSettingsPreferences;
    private final ImportFormatPreferences importFormatPreferences;

    public ExternalFilesContentImporter(GeneralPreferences generalPreferences, ImportSettingsPreferences importSettingsPreferences, ImportFormatPreferences importFormatPreferences) {
        this.generalPreferences = generalPreferences;
        this.importSettingsPreferences = importSettingsPreferences;
        this.importFormatPreferences = importFormatPreferences;
    }

    public ParserResult importPDFContent(Path file) {
        try {
            return new PdfMergeMetadataImporter(importSettingsPreferences, importFormatPreferences).importDatabase(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
           return ParserResult.fromError(e);
        }
    }

    public ParserResult importXMPContent(Path file) {
        return new PdfXmpImporter(importFormatPreferences.getXmpPreferences()).importDatabase(file, StandardCharsets.UTF_8);
    }

    public ParserResult importFromBibFile(Path bibFile, FileUpdateMonitor fileUpdateMonitor) throws IOException {
        return OpenDatabase.loadDatabase(bibFile, generalPreferences, importFormatPreferences, fileUpdateMonitor);
    }
}
