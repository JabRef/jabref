package org.jabref.http.server.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUtils.class);

    private static java.nio.file.Path getLibraryPath(String id, FilesToServe filesToServe) {
        return filesToServe.getFilesToServe()
                           .stream()
                           .filter(p -> (p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p)).equals(id))
                           .findAny()
                           .orElseThrow(NotFoundException::new);
    }

    private static java.nio.file.Path getLibraryPath(String id, SrvStateManager srvStateManager) {
        return srvStateManager.getOpenDatabases()
                              .stream()
                              .filter(context -> context.getDatabasePath().isPresent())
                              .map(context -> context.getDatabasePath().get())
                              .filter(p -> (p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p)).equals(id))
                              .findAny()
                              .orElseThrow(NotFoundException::new);
    }

    /// @throws NotFoundException if no file with the given id is found in either filesToServe or contextsToServe
    public static @NonNull Path getLibraryPath(String id, FilesToServe filesToServe, SrvStateManager srvStateManager) {
        if (filesToServe.isEmpty()) {
            return getLibraryPath(id, srvStateManager);
        } else {
            return getLibraryPath(id, filesToServe);
        }
    }

    /// @param id - also "demo" for the demo library
    /// @throws NotFoundException if no file with the given id is found in either filesToServe or contextsToServe
    public static @NonNull BibDatabaseContext getBibDatabaseContext(String id, FilesToServe filesToServe, SrvStateManager srvStateManager, ImportFormatPreferences importFormatPreferences) throws IOException {
        BibtexImporter bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
        if ("demo".equals(id)) {
            try (InputStream chocolateBibInputStream = BibDatabase.class.getResourceAsStream("/Chocolate.bib")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(chocolateBibInputStream, StandardCharsets.UTF_8));
                return bibtexImporter.importDatabase(reader).getDatabaseContext();
            }
        }

        if (filesToServe.isEmpty()) {
            // empty filesToServe indicates GUI mode

            if ("current".equals(id)) {
                return srvStateManager.getActiveDatabase().orElseThrow(NotFoundException::new);
            }

            return srvStateManager.getOpenDatabases().stream()
                                  .filter(context -> context.getDatabasePath().isPresent())
                                  .filter(context -> {
                                      Path p = context.getDatabasePath().get();
                                      return (p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p)).equals(id);
                                  })
                                  .findFirst()
                                  .orElseThrow(() -> new NotFoundException("No library with id " + id + " found"));
        }

        Path library = getLibraryPath(id, filesToServe);
        try {
            return bibtexImporter.importDatabase(library).getDatabaseContext();
        } catch (IOException e) {
            LOGGER.warn("Could not find open library file {}", library, e);
            throw new InternalServerErrorException("Could not parse library", e);
        }
    }
}
