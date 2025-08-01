package org.jabref.logic.git.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class GitFileWriter {
    private static final ConcurrentHashMap<Path, Object> FILELOCKS = new ConcurrentHashMap<>();

    public static void write(Path file, BibDatabaseContext bibDatabaseContext, ImportFormatPreferences importPrefs) throws IOException {
        Object lock = FILELOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), key -> new Object());

        SelfContainedSaveConfiguration saveConfiguration = new SelfContainedSaveConfiguration();
        Charset encoding = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);

        synchronized (lock) {
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, encoding, saveConfiguration.shouldMakeBackup())) {
                BibWriter bibWriter = new BibWriter(fileWriter, bibDatabaseContext.getDatabase().getNewLineSeparator());
                BibDatabaseWriter writer = new BibDatabaseWriter(
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
