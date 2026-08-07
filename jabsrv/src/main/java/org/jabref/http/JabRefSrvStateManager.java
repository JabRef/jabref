package org.jabref.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.command.CommandSelectionTab;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.search.SearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.inmemory.InMemorySearchBackend;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import io.github.adr.linked.ADR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// [SrvStateManager] for the stand-alone HTTP server.
///
/// The server serves a fixed set of libraries known at startup. Each one is parsed
/// once in the constructor, an in-memory [SearchContext] is built for it, and the
/// `(BibDatabaseContext, mtime)` pair is held for the lifetime of the process.
///
/// Because nothing watches the files asynchronously (jabsrv uses
/// [DummyFileUpdateMonitor] everywhere), [#getOpenDatabases] does a per-call
/// `stat` on every tracked file and re-parses any that changed on disk since the
/// last observation. The cost is one stat per library per request; only an actual
/// modification triggers re-parsing. This is the cheap middle ground between
/// "snapshot at startup" (broken when the user edits a `.bib` externally) and
/// running a real [org.jabref.model.util.FileUpdateMonitor] thread.
///
/// There is no Postgres path: the in-memory backend has no startup cost, and the
/// toggle is hardwired to `false`.
public class JabRefSrvStateManager extends AbstractSrvStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefSrvStateManager.class);

    private final BibEntryPreferences bibEntryPreferences;
    private final BibtexImporter importer;
    private final ObservableList<BibDatabaseContext> openDatabases = FXCollections.observableArrayList();
    private final Map<Path, FileTime> lastModified = new HashMap<>();

    public JabRefSrvStateManager(BibEntryPreferences bibEntryPreferences, ImportFormatPreferences importFormatPreferences, List<Path> files) {
        this.bibEntryPreferences = bibEntryPreferences;
        this.importer = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
        for (Path file : files) {
            parseLibrary(file).ifPresent(context -> {
                openDatabases.add(context);
                registerSearchContext(context);
            });
        }
    }

    /// Returns an unmodifiable snapshot of the open libraries.
    ///
    /// Grizzly/Jersey serve requests concurrently and several resources call
    /// `getOpenDatabases().stream()`. Returning the live [#openDatabases] would
    /// let a caller iterate the list while another thread enters this method
    /// and `refreshStaleLibraries` structurally modifies it via the
    /// `ListIterator#set` in the reparse path. The snapshot is taken under the
    /// monitor so iteration after the call is race-free.
    @Override
    public synchronized ObservableList<BibDatabaseContext> getOpenDatabases() {
        refreshStaleLibraries();
        return FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(openDatabases));
    }

    /// Compares each tracked file's current mtime against the snapshot taken at
    /// the last parse. If it changed, the library is re-parsed, the context in
    /// [#openDatabases] is replaced, the old [SearchContext] is removed, and a
    /// fresh one is registered under the new context's uid.
    @ADR(61)
    private void refreshStaleLibraries() {
        ListIterator<BibDatabaseContext> it = openDatabases.listIterator();
        while (it.hasNext()) {
            BibDatabaseContext current = it.next();
            Optional<Path> pathOpt = current.getDatabasePath();
            if (pathOpt.isEmpty()) {
                continue;
            }
            Path path = pathOpt.get();
            try {
                FileTime now = Files.getLastModifiedTime(path);
                FileTime previous = lastModified.get(path);
                if (now.equals(previous)) {
                    continue;
                }
                parseLibrary(path).ifPresent(updated -> {
                    removeSearchContext(current);
                    it.set(updated);
                    registerSearchContext(updated);
                });
            } catch (IOException e) {
                LOGGER.warn("Could not stat library {} for freshness check", path, e);
            }
        }
    }

    /// Parses a single library. A library that fails to parse is logged and skipped.
    private Optional<BibDatabaseContext> parseLibrary(Path file) {
        try {
            BibDatabaseContext context = importer.importDatabase(file).getDatabaseContext();
            lastModified.put(file, Files.getLastModifiedTime(file));
            return Optional.of(context);
        } catch (IOException e) {
            LOGGER.error("Could not parse library {}", file, e);
            return Optional.empty();
        }
    }

    private void registerSearchContext(BibDatabaseContext database) {
        Supplier<SearchBackend> inMemory = () -> new InMemorySearchBackend(database, bibEntryPreferences);
        SimpleBooleanProperty usePostgres = new SimpleBooleanProperty(false);
        assert !usePostgres.get() : "Stand-alone HTTP server must never use the Postgres backend";
        // The in-memory supplier is wired into the SQL slot too: should the toggle ever
        // flip, an unexpected backend swap stays harmless instead of throwing at runtime.
        setSearchContext(database, new SearchContext(usePostgres, inMemory, inMemory));
    }

    @Override
    public OptionalObjectProperty<BibDatabaseContext> activeDatabaseProperty() {
        return OptionalObjectProperty.empty();
    }

    @Override
    public ObservableList<BibEntry> getSelectedEntries() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public Optional<BibDatabaseContext> getActiveDatabase() {
        return Optional.empty();
    }

    @Override
    public synchronized List<String> getAllDatabasePaths() {
        return openDatabases.stream()
                            .map(BibDatabaseContext::getDatabasePath)
                            .flatMap(Optional::stream)
                            .map(path -> path.toAbsolutePath().toString())
                            .toList();
    }

    @Override
    public ObjectBinding<Optional<CommandSelectionTab>> getActiveSelectionTabProperty() {
        return new ObjectBinding<>() {
            @Override
            protected Optional<CommandSelectionTab> computeValue() {
                return Optional.empty();
            }
        };
    }
}
