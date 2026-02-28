package org.jabref.logic.git.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseSaver;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class GitFileWriter {
    private static final ConcurrentHashMap<Path, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    /// @implNote this should be in sync with {@link org.jabref.gui.exporter.SaveDatabaseAction#saveDatabase}
    public static void write(Path file, BibDatabaseContext bibDatabaseContext, CliPreferences cliPreferences) throws IOException {
        SelfContainedSaveConfiguration saveConfiguration = new SelfContainedSaveConfiguration();
        Charset encoding = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);

        synchronized (FILE_LOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), _ -> new Object())) {
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, encoding, saveConfiguration.shouldMakeBackup())) {
                BibWriter bibWriter = new BibWriter(fileWriter, bibDatabaseContext.getDatabase().getNewLineSeparator());

                BibDatabaseSaver saver = new BibDatabaseSaver(bibWriter, saveConfiguration, cliPreferences, new BibEntryTypesManager());
                saver.saveDatabase(bibDatabaseContext);

                if (fileWriter.hasEncodingProblems()) {
                    throw new IOException("Encoding problem detected when saving .bib file: "
                            + fileWriter.getEncodingProblems().toString());
                }
            }
        }
    }
}
