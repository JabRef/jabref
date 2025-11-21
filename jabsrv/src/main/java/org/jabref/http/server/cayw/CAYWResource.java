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
import java.util.stream.Collectors;

import javafx.application.Platform;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.http.JabRefSrvStateManager;
import org.jabref.http.SrvStateManager;
import org.jabref.http.server.cayw.format.CAYWFormatter;
import org.jabref.http.server.cayw.format.FormatterService;
import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.SearchDialog;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.push.PushToApplications;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;

import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
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
    private SrvStateManager srvStateManager;

    @GET
    public Response getCitation(
            @BeanParam CAYWQueryParams queryParams
    ) throws IOException, ExecutionException, InterruptedException {
        // Probe parameter handling
        if (queryParams.isProbe()) {
            return Response.ok("ready").build();
        }

        BibDatabaseContext databaseContext = getBibDatabaseContext(queryParams);

        // Selected parameter handling
        List<CAYWEntry> searchResults;
        if (queryParams.isSelected()) {
            if (srvStateManager instanceof JabRefSrvStateManager) {
                LOGGER.error("The 'selected' parameter is not supported in CLI mode. Please use the GUI to use the selected entries.");
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("The 'selected' parameter is not supported in CLI mode. Please use the GUI to use the selected entries.")
                               .build();
            }
            searchResults = srvStateManager.getSelectedEntries().stream().map(this::createCAYWEntry).toList();
        } else {
            List<CAYWEntry> entries = databaseContext.getEntries()
                                                     .stream()
                                                     .map(this::createCAYWEntry)
                                                     .collect(Collectors.toList());
            initializeGUI();
            searchResults = openSearchGui(entries);
        }

        if (searchResults.isEmpty()) {
            return Response.noContent().build();
        }

        // Select parameter handling
        if (queryParams.isSelect()) {
            if (srvStateManager instanceof JabRefSrvStateManager) {
                LOGGER.error("The 'select' parameter is not supported in CLI mode. Please use the GUI to select entries.");
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity("The 'select' parameter is not supported in CLI mode. Please use the GUI to select entries.")
                               .build();
            }
            srvStateManager.getActiveSelectionTabProperty().get().ifPresent(selectionTab -> {
                selectionTab.clearAndSelect(searchResults.stream().map(CAYWEntry::bibEntry).collect(Collectors.toList()));
            });
        }

        // Format parameter handling
        CAYWFormatter formatter = formatterService.getFormatter(queryParams);
        String formattedResponse = formatter.format(queryParams, searchResults);

        // Clipboard parameter handling
        if (queryParams.isClipboard()) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard systemClipboard = toolkit.getSystemClipboard();
            StringSelection strSel = new StringSelection(formattedResponse);
            systemClipboard.setContents(strSel, null);
        }

        // Push to Application parameter handling
        if (queryParams.getApplication().isPresent()) {
            CitationCommandString citationCmd = new CitationCommandString("\\".concat(queryParams.getCommand().orElse("autocite")).concat("{"), ",", "}");
            PushToApplications.getApplication(queryParams.getApplication().get(), LOGGER::info, preferences.getPushToApplicationPreferences().withCitationCommand(citationCmd))
                              .ifPresent(application -> application.pushEntries(searchResults.stream().map(CAYWEntry::bibEntry).toList()));
        }

        return Response.ok(formattedResponse).type(formatter.getMediaType()).build();
    }

    private List<CAYWEntry> openSearchGui(List<CAYWEntry> entries) throws InterruptedException, ExecutionException {
        /* unused until DatabaseSearcher is fixed
        PostgreServer postgreServer = new PostgreServer();
        IndexManager.clearOldSearchIndices();
        searcher = new DatabaseSearcher(
                databaseContext,
                new CurrentThreadTaskExecutor(),
                preferences,
                postgreServer);
          */

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

        return future.get();
    }

    private BibDatabaseContext getBibDatabaseContext(CAYWQueryParams queryParams) throws IOException {
        Optional<String> libraryId = queryParams.getLibraryId();
        if (libraryId.isPresent()) {
            return ServerUtils.getBibDatabaseContext(libraryId.get(), filesToServe, srvStateManager, preferences.getImportFormatPreferences());
        }

        Optional<String> libraryPath = queryParams.getLibraryPath();
        if (libraryPath.isPresent() && "demo".equals(libraryPath.get())) {
            return ServerUtils.getBibDatabaseContext("demo", filesToServe, srvStateManager, preferences.getImportFormatPreferences());
        }

        if (libraryPath.isPresent()) {
            assert !"demo".equalsIgnoreCase(libraryPath.get());
            InputStream inputStream = getDatabaseStreamFromPath(java.nio.file.Path.of(libraryPath.get()));
            return getDatabaseContextFromStream(inputStream);
        }

        if (srvStateManager.getActiveDatabase().isPresent()) {
            return srvStateManager.getActiveDatabase().get();
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
        BibtexImporter bibtexImporter = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor());
        BibDatabaseContext databaseContext;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            databaseContext = bibtexImporter.importDatabase(reader).getDatabaseContext();
        }
        return databaseContext;
    }

    private synchronized void initializeGUI() {
        // TODO: Implement a better way to handle the window popup since this is a bit hacky.
        if (!initialized) {
            if (!(srvStateManager instanceof JabRefSrvStateManager)) {
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
        return entry.label().toLowerCase().contains(lowerSearchText) ||
                entry.description().toLowerCase().contains(lowerSearchText) ||
                entry.shortLabel().toLowerCase().contains(lowerSearchText);
    }
}
