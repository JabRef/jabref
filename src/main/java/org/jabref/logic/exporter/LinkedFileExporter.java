package org.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;

public class LinkedFileExporter extends ExportFormat {

    public LinkedFileExporter() {
        super("Files Exporter", "Files", null, null, ".*");

    }

    @Override
    public void performExport(final BibDatabaseContext databaseContext, String file, final Charset encoding,
            List<BibEntry> entries) throws Exception {

        for (BibEntry entry : entries) {
            TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);

            List<ParsedFileField> files = typedEntry.getFiles();
            for (ParsedFileField fileEntry : files) {
                String fileName = fileEntry.getLink();

                Optional<Path> fileToExport = FileUtil.expandFilename(fileName,
                        databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences()))
                        .map(File::toPath);

                fileToExport.ifPresent(f -> {
                    Path newFilePath = Paths.get(file.replace(".*", "")).getParent().resolve(f.getFileName());
                    FileUtil.copyFile(f, newFilePath, false);
                });

            }

        }

    }

}
