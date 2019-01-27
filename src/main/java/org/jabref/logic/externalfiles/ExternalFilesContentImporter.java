package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

public class ExternalFilesContentImporter {

    private final PdfContentImporter pdfImporter;
    private final ImportFormatPreferences importFormatPreferences;

    public ExternalFilesContentImporter(ImportFormatPreferences importFormatPreferences) {
        pdfImporter = new PdfContentImporter(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
    }

    public List<BibEntry> importPDFContent(Path file) {
        return pdfImporter.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

    }

    public List<BibEntry> importXMPContent(Path file) throws IOException {
        return XmpUtilReader.readXmp(file, importFormatPreferences.getXmpPreferences());

    }

    public List<BibEntry> importFromBibFile(Path bibFile, FileUpdateMonitor fileUpdateMonitor) {
        ParserResult parserResult = OpenDatabase.loadDatabase(bibFile.toString(), importFormatPreferences, fileUpdateMonitor);
        return parserResult.getDatabaseContext().getEntries();
    }
}
