package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.util.FileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class XmpPdfExporter extends Exporter {

    private final XmpPreferences xmpPreferences;

    public XmpPdfExporter(XmpPreferences xmpPreferences) {
        super("pdf", FileType.PDF_XMP.getDescription(), FileType.PDF_XMP);
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path pdfFile, Charset encoding, List<BibEntry> entries) throws Exception {

        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(pdfFile);
        Objects.requireNonNull(entries);

        if (pdfFile.toString().endsWith(".pdf")) {
            XmpUtilWriter.writeXmp(pdfFile, entries, databaseContext.getDatabase(), xmpPreferences);
        }
    }

}
