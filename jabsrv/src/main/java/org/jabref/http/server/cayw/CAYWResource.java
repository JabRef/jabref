package org.jabref.http.server.cayw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;

import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.SearchDialog;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("better-bibtex/cayw")
public class CAYWResource {
    public static final Logger LOGGER = LoggerFactory.getLogger(CAYWResource.class);
    private static final String CHOCOLATEBIB_PATH = "/Chocolate.bib";
    private static boolean initialized = false;

    @Inject
    private CliPreferences preferences;

    @Inject
    private Gson gson;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCitation(
            @QueryParam("probe") String probe,
            @QueryParam("format") @DefaultValue("latex") String format,
            @QueryParam("clipboard") String clipboard,
            @QueryParam("minimize") String minimize,
            @QueryParam("texstudio") String texstudio,
            @QueryParam("selected") String selected,
            @QueryParam("select") String select,
            @QueryParam("librarypath") String libraryPath
    ) throws IOException, ExecutionException, InterruptedException {
        if (probe != null && !probe.isEmpty()) {
            return Response.ok("ready").build();
        }

        BibDatabaseContext databaseContext = getBibDatabaseContext(libraryPath);

        /* unused until DatabaseSearcher is fixed
        PostgreServer postgreServer = new PostgreServer();
        IndexManager.clearOldSearchIndices();
        searcher = new DatabaseSearcher(
                databaseContext,
                new CurrentThreadTaskExecutor(),
                preferences,
                postgreServer);
          */

        List<CAYWEntry<BibEntry>> entries = databaseContext.getEntries()
                                 .stream()
                                 .map(this::createCAYWEntry)
                                 .toList();

        initializeGUI();

        CompletableFuture<List<BibEntry>> future = new CompletableFuture<>();
        Platform.runLater(() -> {
                SearchDialog<BibEntry> dialog = new SearchDialog<>();
                // TODO: Using the DatabaseSearcher directly here results in a lot of exceptions being thrown, so we use an alternative for now until we have a nice way of using the DatabaseSearcher class.
                //       searchDialog.set(new SearchDialog<>(s -> searcher.getMatches(new SearchQuery(s)), entries));
            List<BibEntry> results = dialog.show(searchQuery ->
                    entries.stream()
                           .filter(bibEntryCAYWEntry -> matches(bibEntryCAYWEntry, searchQuery))
                           .map(CAYWEntry::getValue)
                           .toList(),
                    entries);

                future.complete(results);
        });

        List<String> citationKeys = future.get().stream()
                .map(BibEntry::getCitationKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (citationKeys.isEmpty()) {
            return Response.noContent().build();
        }

        return Response.ok(gson.toJson(citationKeys)).build();
    }

    private BibDatabaseContext getBibDatabaseContext(String libraryPath) throws IOException {
        InputStream libraryStream;
        if (libraryPath != null && !libraryPath.isEmpty()) {
            java.nio.file.Path path = java.nio.file.Path.of(libraryPath);
            if (!Files.exists(path)) {
                LOGGER.error("Library path does not exist, using the default chocolate.bib: {}", libraryPath);
                libraryStream = getChocolateBibAsStream();
            } else {
                libraryStream = Files.newInputStream(path);
            }
        } else {
            // Use the latest opened library as the default library
            final List<java.nio.file.Path> lastOpenedLibraries = new ArrayList<>(JabRefCliPreferences.getInstance().getLastFilesOpenedPreferences().getLastFilesOpened());
            if (lastOpenedLibraries.isEmpty()) {
                LOGGER.warn("No library path provided and no last opened libraries found, using the default chocolate.bib.");
                libraryStream = getChocolateBibAsStream();
            } else {
                java.nio.file.Path lastOpenedLibrary = lastOpenedLibraries.getFirst();
                if (!Files.exists(lastOpenedLibrary)) {
                    LOGGER.error("Last opened library does not exist, using the default chocolate.bib: {}", lastOpenedLibrary);
                    libraryStream = getChocolateBibAsStream();
                } else {
                    libraryStream = Files.newInputStream(lastOpenedLibrary);
                }
            }
        }

        BibtexImporter bibtexImporter = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        BibDatabaseContext databaseContext;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(libraryStream, StandardCharsets.UTF_8))) {
            databaseContext = bibtexImporter.importDatabase(reader).getDatabaseContext();
        }
        return databaseContext;
    }

    private synchronized void initializeGUI() {
        // TODO: Implement a better way to handle the window popup since this is a bit hacky.
        if (!initialized) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                initialized = true;
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("JavaFX initialization interrupted", e);
            }
        }
    }

    /// @return a stream to the `Chocolate.bib` file in the classpath (is null only if the file was moved or there are issues with the classpath)
    private @Nullable InputStream getChocolateBibAsStream() {
        return BibDatabase.class.getResourceAsStream(CHOCOLATEBIB_PATH);
    }

    private CAYWEntry<BibEntry> createCAYWEntry(BibEntry entry) {
        String label = entry.getCitationKey().orElse("");
        String shortLabel = label;
        String description = entry.getField(StandardField.TITLE).orElse(entry.getAuthorTitleYear());
        return new CAYWEntry<>(entry, label, shortLabel, description);
    }

    private boolean matches(CAYWEntry<BibEntry> entry, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String lowerSearchText = searchText.toLowerCase();
        return entry.getLabel().toLowerCase().contains(lowerSearchText) ||
                entry.getDescription().toLowerCase().contains(lowerSearchText) ||
                entry.getShortLabel().toLowerCase().contains(lowerSearchText);
    }
}
