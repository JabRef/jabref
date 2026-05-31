package org.jabref.http.server.cayw;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

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
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.push.PushToApplications;
import org.jabref.logic.search.inmemory.InMemoryLibrarySearcher;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.DummyFileUpdateMonitor;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
@AllowedToUseAwt("Requires java.awt.datatransfer.Clipboard")
@jakarta.ws.rs.Path("better-bibtex/cayw")
public class CAYWResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CAYWResource.class);
    private static final String CHOCOLATEBIB_PATH = "/Chocolate.bib";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String CAYW_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String NO_SNIFF = "nosniff";
    private static final String INVALID_LIBRARY_PATH_ERROR = "The 'librarypath' parameter must reference a currently served library file.";
    private static volatile boolean allowAllLibraryPaths = false;
    private static final Set<Path> TRUSTED_LIBRARY_PATHS = ConcurrentHashMap.newKeySet();
    private static final Set<Path> BLOCKED_LIBRARY_PATHS = ConcurrentHashMap.newKeySet();
    private static boolean initialized = false;
    private static final Pattern ALLOWED_COMMAND = Pattern.compile("[a-zA-Z*]+");

    @Inject
    private CliPreferences preferences;

    @Inject
    private FormatterService formatterService;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private SrvStateManager srvStateManager;

    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response getCitation(
            @BeanParam CAYWQueryParams queryParams
    ) throws IOException, ExecutionException, InterruptedException {
        // Validation of command parameter
        String command = queryParams.getCommand().orElse("autocite");
        if (!ALLOWED_COMMAND.matcher(command).matches()) {
            LOGGER.warn("Blocked CAYW request with malicious command: {}", command);
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("The 'command' parameter contains invalid characters. Only letters (A–Z, a–z) and '*' are allowed.")
                           .build();
        }

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
            searchResults = openSearchGui(entries, databaseContext);
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
            CitationCommandString citationCmd = new CitationCommandString("\\" + command + "{", ",", "}");
            PushToApplications.getApplication(queryParams.getApplication().get(), LOGGER::info, preferences.getPushToApplicationPreferences().withCitationCommand(citationCmd))
                              .ifPresent(application -> application.pushEntries(searchResults.stream().map(CAYWEntry::bibEntry).toList()));
        }

        return Response.ok(formattedResponse)
                       .type(formatter.getMediaType())
                       .header(X_CONTENT_TYPE_OPTIONS, NO_SNIFF)
                       .header(CONTENT_SECURITY_POLICY, CAYW_CONTENT_SECURITY_POLICY)
                       .build();
    }

    private List<CAYWEntry> openSearchGui(List<CAYWEntry> entries, BibDatabaseContext databaseContext) throws InterruptedException, ExecutionException {
        InMemoryLibrarySearcher searcher = new InMemoryLibrarySearcher(databaseContext, preferences.getBibEntryPreferences());

        CompletableFuture<List<CAYWEntry>> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            SearchDialog dialog = new SearchDialog();
            List<CAYWEntry> results = dialog.show(
                    searchText -> filterEntries(entries, searchText, searcher),
                    entries);
            future.complete(results);
        });

        return future.get();
    }

    /// Filter strategy:
    /// - Empty input → return everything.
    /// - Valid Search.g4 expression → grammar-based filter via [InMemoryLibrarySearcher].
    /// - Invalid expression (e.g. user is mid-typing `author=`) → fall back to a plain
    ///   substring match across the CAYW labels so the list stays useful as the user types.
    private List<CAYWEntry> filterEntries(List<CAYWEntry> entries, String searchText, InMemoryLibrarySearcher searcher) {
        if (searchText.isEmpty()) {
            return entries;
        }
        SearchQuery query = new SearchQuery(searchText);
        if (query.isValid()) {
            return entries.stream()
                          .filter(caywEntry -> searcher.matches(caywEntry.bibEntry(), query))
                          .toList();
        }
        return entries.stream()
                      .filter(caywEntry -> matchesSubstring(caywEntry, searchText))
                      .toList();
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
            Path requestedLibraryPath = normalizeLibraryPath(libraryPath.get());
            if (!isServedLibraryPath(requestedLibraryPath) && !isLibraryPathAccessAllowed(requestedLibraryPath)) {
                throw new BadRequestException(INVALID_LIBRARY_PATH_ERROR);
            }
            InputStream inputStream = getDatabaseStreamFromPath(requestedLibraryPath);
            return getDatabaseContextFromStream(inputStream);
        }

        if (srvStateManager.getActiveDatabase().isPresent()) {
            return srvStateManager.getActiveDatabase().get();
        }

        return getDatabaseContextFromStream(getLatestDatabaseStream());
    }

    private Path normalizeLibraryPath(String libraryPath) {
        try {
            return Path.of(libraryPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new BadRequestException(INVALID_LIBRARY_PATH_ERROR, exception);
        }
    }

    private boolean isServedLibraryPath(Path requestedLibraryPath) {
        return getServedLibraryPaths().stream().anyMatch(requestedLibraryPath::equals);
    }

    private boolean isLibraryPathAccessAllowed(Path requestedLibraryPath) {
        if (srvStateManager instanceof JabRefSrvStateManager) {
            return true;
        }

        if (allowAllLibraryPaths || TRUSTED_LIBRARY_PATHS.contains(requestedLibraryPath)) {
            return true;
        }

        if (BLOCKED_LIBRARY_PATHS.contains(requestedLibraryPath)) {
            return false;
        }

        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.warn("Rejecting CAYW library path access in headless mode: {}", requestedLibraryPath);
            return false;
        }

        return promptForLibraryPathAccess(requestedLibraryPath);
    }

    private boolean promptForLibraryPathAccess(Path requestedLibraryPath) {
        // See #15295: security hardening for browser-extension communication while preserving CAYW usability.
        try {
            initializeGUI();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not show CAYW security prompt for path {}.", requestedLibraryPath, exception);
            return false;
        }

        CompletableFuture<LibraryPathAccessPromptResult> future = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(Localization.lang("Security warning"));
                alert.setHeaderText(Localization.lang("You are about to open a local file."));
                Label fileLabel = new Label(Localization.lang("File: %0", requestedLibraryPath));
                CheckBox dontAskAgain = new CheckBox(Localization.lang("Do not ask again"));
                alert.getDialogPane().setContent(new VBox(10, fileLabel, dontAskAgain));

                ButtonType allowButton = new ButtonType(Localization.lang("Allow"), ButtonBar.ButtonData.YES);
                ButtonType allowAllButton = new ButtonType(Localization.lang("Allow all"), ButtonBar.ButtonData.APPLY);
                ButtonType disallowButton = new ButtonType(Localization.lang("Disallow"), ButtonBar.ButtonData.NO);
                alert.getButtonTypes().setAll(allowButton, allowAllButton, disallowButton);

                ButtonType selectedButton = alert.showAndWait().orElse(disallowButton);
                future.complete(new LibraryPathAccessPromptResult(selectedButton, dontAskAgain.isSelected()));
            });
        } catch (IllegalStateException exception) {
            LOGGER.warn("JavaFX toolkit not initialized for CAYW security prompt.", exception);
            return false;
        }

        try {
            LibraryPathAccessPromptResult promptResult = future.get();
            if (promptResult.selectedButton().getButtonData() == ButtonBar.ButtonData.APPLY) {
                allowAllLibraryPaths = true;
                return true;
            }

            boolean shouldAllow = promptResult.selectedButton().getButtonData() == ButtonBar.ButtonData.YES;
            if (promptResult.dontAskAgain()) {
                if (shouldAllow) {
                    TRUSTED_LIBRARY_PATHS.add(requestedLibraryPath);
                } else {
                    BLOCKED_LIBRARY_PATHS.add(requestedLibraryPath);
                }
            }
            return shouldAllow;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for CAYW security prompt.", exception);
        } catch (ExecutionException exception) {
            LOGGER.warn("Failed to evaluate CAYW security prompt.", exception);
        }
        return false;
    }

    private List<Path> getServedLibraryPaths() {
        List<Path> servedLibraries = Optional.ofNullable(filesToServe.getFilesToServe()).orElse(List.of()).stream()
                                             .map(path -> path.toAbsolutePath().normalize())
                                             .toList();
        if (!servedLibraries.isEmpty()) {
            return servedLibraries;
        }

        return srvStateManager.getOpenDatabases().stream()
                              .map(BibDatabaseContext::getDatabasePath)
                              .flatMap(Optional::stream)
                              .map(path -> path.toAbsolutePath().normalize())
                              .toList();
    }

    private InputStream getLatestDatabaseStream() throws IOException {
        InputStream libraryStream;
        // Use the latest opened library as the default library
        final List<Path> lastOpenedLibraries = new ArrayList<>(preferences.getLastFilesOpenedPreferences().getLastFilesOpened());
        if (lastOpenedLibraries.isEmpty()) {
            LOGGER.warn("No library path provided and no last opened libraries found, using the default chocolate.bib.");
            libraryStream = getChocolateBibAsStream();
        } else {
            Path lastOpenedLibrary = lastOpenedLibraries.getFirst();
            if (!Files.exists(lastOpenedLibrary)) {
                LOGGER.error("Last opened library does not exist, using the default chocolate.bib: {}", lastOpenedLibrary);
                libraryStream = getChocolateBibAsStream();
            } else {
                libraryStream = Files.newInputStream(lastOpenedLibrary);
            }
        }
        return libraryStream;
    }

    private InputStream getDatabaseStreamFromPath(Path path) throws IOException {
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
        if (initialized) {
            return;
        }
        if (!(srvStateManager instanceof JabRefSrvStateManager)) {
            LOGGER.debug("Running inside JabRef UI, no need to initialize JavaFX for CAYW resource.");
            initialized = true;
            return;
        }
        LOGGER.debug("Initializing JavaFX for CAYW resource.");
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
        } catch (IllegalStateException alreadyInitialized) {
            LOGGER.debug("JavaFX runtime already initialized.", alreadyInitialized);
            initialized = true;
            return;
        } catch (Throwable e) {
            // Catches NoClassDefFoundError/UnsatisfiedLinkError when the JavaFX runtime is missing or version-mismatched
            // (e.g., javafx.graphics from an older version paired with a newer javafx.base where javafx.util.FXPermission was removed).
            LOGGER.error("Could not initialize JavaFX runtime for CAYW resource.", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("CAYW unavailable: JavaFX runtime could not be initialized (" + e.getClass().getName() + ": " + e.getMessage() + "). The CAYW endpoint requires a working, version-consistent JavaFX runtime on the module path.")
                            .build());
        }
        try {
            latch.await();
            initialized = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("JavaFX initialization interrupted", e);
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
        return new CAYWEntry(entry, label, shortLabel, description, new CitationProperties());
    }

    private boolean matchesSubstring(CAYWEntry entry, String searchText) {
        String lowerSearchText = searchText.toLowerCase();
        return entry.label().toLowerCase().contains(lowerSearchText) ||
                entry.description().toLowerCase().contains(lowerSearchText) ||
                entry.shortLabel().toLowerCase().contains(lowerSearchText);
    }

    private record LibraryPathAccessPromptResult(ButtonType selectedButton, boolean dontAskAgain) {
    }
}
