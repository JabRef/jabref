package org.jabref.http.server.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.common.html.HtmlEscapers;
import jakarta.ws.rs.NotFoundException;
import org.jspecify.annotations.NonNull;

public class ServerUtils {

    /// Returns ids of all libraries the state manager currently considers
    /// open. Used by every resource that operates across the open
    /// collection (libraries listing, batch query, ...).
    public static List<String> openLibraryIds(SrvStateManager srvStateManager) {
        return srvStateManager.getOpenDatabases().stream()
                              .map(BibDatabaseContext::getDatabasePath)
                              .flatMap(Optional::stream)
                              .map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                              .toList();
    }

    /// Returns the on-disk path of the library with the given id, looking it up in the
    /// state manager's open databases (the same source as [#getBibDatabaseContext]).
    ///
    /// @throws NotFoundException if no library with the given id is found
    public static @NonNull Path getLibraryPath(String id, SrvStateManager srvStateManager) {
        return srvStateManager.getOpenDatabases()
                              .stream()
                              .map(BibDatabaseContext::getDatabasePath)
                              .flatMap(java.util.Optional::stream)
                              .filter(p -> (p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p)).equals(id))
                              .findAny()
                              .orElseThrow(NotFoundException::new);
    }

    /// Returns the {@link BibDatabaseContext} for the given library id.
    ///
    /// Looks up the context from the state manager's open databases. In stand-alone server
    /// mode those are parsed once at startup and held for the lifetime of the process; in
    /// GUI mode they are the user's open library tabs. Either way, the returned context is
    /// the *same* object the state manager registered a {@link org.jabref.logic.search.SearchContext}
    /// for, so callers can pass it straight to {@link SrvStateManager#getSearchContext}.
    ///
    /// @param id - also "demo" for the bundled Chocolate.bib demo library, and "current" for the active GUI database
    /// @throws NotFoundException if no library with the given id is found
    public static @NonNull BibDatabaseContext getBibDatabaseContext(String id, SrvStateManager srvStateManager, ImportFormatPreferences importFormatPreferences) throws IOException {
        if ("demo".equals(id)) {
            BibtexImporter bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
            try (InputStream chocolateBibInputStream = BibDatabase.class.getResourceAsStream("/Chocolate.bib")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(chocolateBibInputStream, StandardCharsets.UTF_8));
                return bibtexImporter.importDatabase(reader).getDatabaseContext();
            }
        }
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
                              .orElseThrow(() -> new NotFoundException("No library with id " + HtmlEscapers.htmlEscaper().escape(id) + " found"));
    }
}
