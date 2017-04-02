package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.util.List;
import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;

public class PdfFileExporter extends ExportFormat {

    public PdfFileExporter() {
        super("PDF files", "PDF", null, null, ".pdf");
    }
    @Override
    public void performExport(final BibDatabaseContext databaseContext, String file, final Charset encoding,
            List<BibEntry> entries) throws Exception {

        for (BibEntry entry : entries) {
            TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);

            List<ParsedFileField> files = typedEntry.getFiles();
            for (ParsedFileField fileEntry : files) {
                String fileName = fileEntry.getLink();

                //    databaseContext.getFileDirectories(preferences)
                //   Optional<File> oldFile = FileUtil.expandFilename(fileName,
                //         databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()));

                System.out.println("Export pdfs");
                //  FileUtil.copyFile(oldFile.get().toPath(), file, false);

            }

        }

    }

}
