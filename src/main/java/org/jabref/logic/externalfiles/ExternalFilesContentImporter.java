package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.EpubImporter;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

public class ExternalFilesContentImporter {

    private final ImportFormatPreferences importFormatPreferences;

    public ExternalFilesContentImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public ParserResult importPDFContent(Path file, BibDatabaseContext context, FilePreferences filePreferences) {
        try {
            return new PdfMergeMetadataImporter(importFormatPreferences).importDatabase(file, context, filePreferences);
        } catch (IOException e) {
           return ParserResult.fromError(e);
        }
    }

    public ParserResult importEpubContent(Path file) {
        try {
            return new EpubImporter(importFormatPreferences).importDatabase(file);
        } catch (IOException | XPathExpressionException | ParserConfigurationException e) {
            return ParserResult.fromError(e);
        }
    }

    public ParserResult importFromBibFile(Path bibFile, FileUpdateMonitor fileUpdateMonitor) throws IOException {
        return OpenDatabase.loadDatabase(bibFile, importFormatPreferences, fileUpdateMonitor);
    }
}
