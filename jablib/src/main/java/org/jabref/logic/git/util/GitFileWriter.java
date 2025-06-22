package org.jabref.logic.git.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class GitFileWriter {
    public static void write(Path file, BibDatabaseContext bibDatabaseContext, ImportFormatPreferences importPrefs) throws IOException {
        SelfContainedSaveConfiguration saveConfiguration = new SelfContainedSaveConfiguration();
        Charset encoding = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);

        synchronized (bibDatabaseContext) {
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, encoding, saveConfiguration.shouldMakeBackup())) {
                BibWriter bibWriter = new BibWriter(fileWriter, bibDatabaseContext.getDatabase().getNewLineSeparator());
                BibtexDatabaseWriter writer = new BibtexDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        importPrefs.fieldPreferences(),
                        importPrefs.citationKeyPatternPreferences(),
                        new BibEntryTypesManager()
                );
                writer.saveDatabase(bibDatabaseContext);

                if (fileWriter.hasEncodingProblems()) {
                    throw new IOException("Encoding problem detected when saving .bib file: "
                            + fileWriter.getEncodingProblems().toString());
                }
            }
        }
    }
}
