package org.jabref.http.server.cayw;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.http.server.cayw.format.FormatterService;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.SearchDialog;
import org.jabref.http.server.services.ContextsToServe;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseAwt("Requires java.awt.datatransfer.Clipboard")
@Path("better-bibtex/cayw")
public class CAYWResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CAYWResource.class);
    private static final String CHOCOLATEBIB_PATH = "/Chocolate.bib";
    private static boolean initialized = false;

    @Inject
    private CliPreferences preferences;

    @Inject
    private FormatterService formatterService;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private ContextsToServe contextsToServe;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCitation(
            @BeanParam CAYWQueryParams queryParams
    ) throws IOException, ExecutionException, InterruptedException {
        if (queryParams.isProbe()) {
            return Response.ok("ready").build();
        }

        BibDatabaseContext databaseContext = getBibDatabaseContext(queryParams);

        /* unused until DatabaseSearcher is fixed
        PostgreServer postgreServer = new PostgreServer();
        IndexManager.clearOldSearchIndices();
        searcher = new DatabaseSearcher(
                databaseContext,
                new CurrentThreadTaskExecutor(),
                preferences,
                postgreServer);
          */

        List<CAYWEntry> entries = databaseContext.getEntries()
                                 .stream()
                                 .map(this::createCAYWEntry)
                                 .toList();

        initializeGUI();

        CompletableFuture<List<CAYWEntry>> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            SearchDialog dialog = new SearchDialog();
            // TODO: Using the DatabaseSearcher directly here results in a lot of exceptions being thrown, so we use an alternative for now until we have a nice way of using the DatabaseSearcher class.
            //       searchDialog.set(new SearchDialog<>(s -> searcher.getMatches(new SearchQuery(s)), entries));
            List<CAYWEntry> results = dialog.show(
                    searchQuery ->
                            entries.stream()
                                   .filter(caywEntry -> matches(caywEntry, searchQuery)).toList(),
                    entries);
            future.complete(results);
        });

        List<CAYWEntry> searchResults = future.get();

        if (searchResults.isEmpty()) {
            return Response.noContent().build();
        }

        // Format parameter handling
        String response = formatterService.format(queryParams, searchResults);

        // Clipboard parameter handling
        if (queryParams.isClipboard()) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard systemClipboard = toolkit.getSystemClipboard();
            StringSelection strSel = new StringSelection(response);
            systemClipboard.setContents(strSel, null);
        }

        return Response.ok(response).build();
    }

    private BibDatabaseContext getBibDatabaseContext(CAYWQueryParams queryParams) throws IOException {
        Optional<String> libraryId = queryParams.getLibraryId();
        if (libraryId.isPresent()) {
            if ("demo".equals(libraryId.get())) {
                return ServerUtils.getBibDatabaseContext("demo", filesToServe, contextsToServe, preferences.getImportFormatPreferences());
            }
            return ServerUtils.getBibDatabaseContext(libraryId.get(), filesToServe, contextsToServe, preferences.getImportFormatPreferences());
        }

        Optional<String> libraryPath = queryParams.getLibraryPath();
        if (libraryPath.isPresent() && "demo".equals(libraryPath.get())) {
            return ServerUtils.getBibDatabaseContext("demo", filesToServe, contextsToServe, preferences.getImportFormatPreferences());
        }

        if (queryParams.getLibraryPath().isPresent()) {
            assert !"demo".equalsIgnoreCase(queryParams.getLibraryPath().get());
            InputStream inputStream = getDatabaseStreamFromPath(java.nio.file.Path.of(queryParams.getLibraryPath().get()));
            return getDatabaseContextFromStream(inputStream);
        }

        return getDatabaseContextFromStream(getLatestDatabaseStream());
    }

    private InputStream getLatestDatabaseStream() throws IOException {
        InputStream libraryStream;
        // Use the latest opened library as the default library
        final List<java.nio.file.Path> lastOpenedLibraries = new ArrayList<>(preferences.getLastFilesOpenedPreferences().getLastFilesOpened());
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
        return libraryStream;
    }

    private InputStream getDatabaseStreamFromPath(java.nio.file.Path path) throws IOException {
        if (!Files.exists(path)) {
            LOGGER.warn("The provided library path does not exist: {}. Using the default chocolate.bib.", path);
            return getChocolateBibAsStream();
        }
        return Files.newInputStream(path);
    }

    private BibDatabaseContext getDatabaseContextFromStream(InputStream inputStream) throws IOException {
        BibtexImporter bibtexImporter = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        BibDatabaseContext databaseContext;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            databaseContext = bibtexImporter.importDatabase(reader).getDatabaseContext();
        }
        return databaseContext;
    }

    private synchronized void initializeGUI() {
        // TODO: Implement a better way to handle the window popup since this is a bit hacky.
        if (!initialized) {
            if (!contextsToServe.isEmpty()) {
                LOGGER.debug("Running inside JabRef UI, no need to initialize JavaFX for CAYW resource.");
                initialized = true;
                return;
            }
            LOGGER.debug("Initializing JavaFX for CAYW resource.");
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

    private CAYWEntry createCAYWEntry(BibEntry entry) {
        String label = entry.getCitationKey().orElse("");
        String shortLabel = label;
        String description = entry.getField(StandardField.TITLE).orElse(entry.getAuthorTitleYear());
        return new CAYWEntry(entry, label, shortLabel, description);
    }

    private boolean matches(CAYWEntry entry, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        String lowerSearchText = searchText.toLowerCase();
        return entry.getLabel().toLowerCase().contains(lowerSearchText) ||
                entry.getDescription().toLowerCase().contains(lowerSearchText) ||
                entry.getShortLabel().toLowerCase().contains(lowerSearchText);
    }
}
