package org.jabref.http.server.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.directorylibrary.DirectoryLibrarySynchronizer;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.PlainCitationParserFactory;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.common.html.HtmlEscapers;
import jakarta.ws.rs.NotFoundException;
import org.jspecify.annotations.NonNull;

public class ServerUtils {

    /// The stable id used in URLs: derived from the `.bib` file of a regular library, or from
    /// the root directory of a directory library (which has no `.bib` path of its own) — the
    /// same identity the GUI session store uses, so a directory library keeps one id across
    /// everything. Empty for unsaved libraries.
    /// [impl->req~directory-library.rest-api~1]
    public static Optional<String> libraryId(BibDatabaseContext context) {
        return context.getPathOnDisk().map(ServerUtils::libraryId);
    }

    private static String libraryId(Path path) {
        return path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path);
    }

    /// The file holding the library's BibTeX for endpoints that read or derive files: the
    /// `.bib` itself, or, for a directory library, its `.bib` mirror inside the root.
    ///
    /// @throws NotFoundException if no library with the given id is found
    public static @NonNull Path getLibraryFile(String id, SrvStateManager srvStateManager) {
        Path path = getLibraryPath(id, srvStateManager);
        return Files.isDirectory(path) ? path.resolve(DirectoryLibrarySynchronizer.mirrorFileName(path)) : path;
    }

    /// Returns ids of all libraries the state manager currently considers
    /// open. Used by every resource that operates across the open
    /// collection (libraries listing, batch query, ...).
    public static List<String> openLibraryIds(SrvStateManager srvStateManager) {
        return srvStateManager.getOpenDatabases().stream()
                              .map(ServerUtils::libraryId)
                              .flatMap(Optional::stream)
                              .toList();
    }

    /// Returns the on-disk path of the library with the given id, looking it up in the
    /// state manager's open databases (the same source as [#getBibDatabaseContext]). For a
    /// directory library this is its root directory, which the GUI append command routes back
    /// to the open directory-library tab.
    ///
    /// @throws NotFoundException if no library with the given id is found
    public static @NonNull Path getLibraryPath(String id, SrvStateManager srvStateManager) {
        return srvStateManager.getOpenDatabases()
                              .stream()
                              .map(BibDatabaseContext::getPathOnDisk)
                              .flatMap(Optional::stream)
                              .filter(p -> libraryId(p).equals(id))
                              .findAny()
                              .orElseThrow(NotFoundException::new);
    }

    /// Returns the [BibDatabaseContext] for the given library id.
    ///
    /// Looks up the context from the state manager's open databases. In stand-alone server
    /// mode those are parsed once at startup and held for the lifetime of the process; in
    /// GUI mode they are the user's open library tabs. Either way, the returned context is
    /// the *same* object the state manager registered a [org.jabref.logic.search.SearchContext]
    /// for, so callers can pass it straight to [SrvStateManager#getSearchContext].
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
                              .filter(context -> libraryId(context).filter(id::equals).isPresent())
                              .findFirst()
                              .orElseThrow(() -> new NotFoundException("No library with id " + HtmlEscapers.htmlEscaper().escape(id) + " found"));
    }

    /// Parses a single plain-text bibliography reference into a [BibEntry] using the
    /// plain-citation parser the user selected in preferences (including the LLM parser).
    ///
    /// Shared by the `entries` and `citations` resources so the parser wiring stays in one place.
    public static Optional<BibEntry> parsePlainCitation(CliPreferences preferences, String citationText) throws FetcherException {
        PlainCitationParserChoice choice = preferences.getImporterPreferences().getDefaultPlainCitationParser();
        if (choice == PlainCitationParserChoice.LLM) {
            // The LLM parser needs a ChatModel; build one for this request and
            // close it afterwards so the underlying HTTP client is released.
            try (ChatModel chatModel = ChatModelFactory.create(preferences.getAiPreferences())) {
                return PlainCitationParserFactory.getLlmPlainCitationParser(
                                                         preferences.getImportFormatPreferences(),
                                                         preferences.getAiPreferences(),
                                                         chatModel)
                                                 .parsePlainCitation(citationText);
            }
        }
        return PlainCitationParserFactory.getPlainCitationParser(
                choice,
                preferences.getCitationKeyPatternPreferences(),
                preferences.getGrobidPreferences(),
                preferences.getImportFormatPreferences()).parsePlainCitation(citationText);
    }
}
