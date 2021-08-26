package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.util.FileUpdateMonitor;

public class ExternalFilesContentImporter {

    private final Supplier<ImportSettingsPreferences> importSettingsPreferencesSupplier;
    private final ImportFormatPreferences importFormatPreferences;
    private final TimestampPreferences timestampPreferences;

    public ExternalFilesContentImporter(Supplier<ImportSettingsPreferences> importSettingsPreferencesSupplier, ImportFormatPreferences importFormatPreferences, TimestampPreferences timestampPreferences) {
        this.importSettingsPreferencesSupplier = importSettingsPreferencesSupplier;
        this.importFormatPreferences = importFormatPreferences;
        this.timestampPreferences = timestampPreferences;
    }

    public ParserResult importPDFContent(Path file) {
        try {
            return new PdfMergeMetadataImporter(importSettingsPreferencesSupplier, importFormatPreferences).importDatabase(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
           return ParserResult.fromError(e);
        }
    }

    public ParserResult importXMPContent(Path file) {
        return new PdfXmpImporter(importFormatPreferences.getXmpPreferences()).importDatabase(file, StandardCharsets.UTF_8);
    }

    public ParserResult importFromBibFile(Path bibFile, FileUpdateMonitor fileUpdateMonitor) throws IOException {
        return OpenDatabase.loadDatabase(bibFile, importFormatPreferences, timestampPreferences, fileUpdateMonitor);
    }
}
