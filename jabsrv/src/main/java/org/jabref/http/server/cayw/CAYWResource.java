package org.jabref.http.server.cayw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;

import org.jabref.http.server.cayw.gui.CAYWEntry;
import org.jabref.http.server.cayw.gui.SearchDialog;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
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
    private static boolean initialized = false;

    @Inject
    private CliPreferences preferences;

    @Inject
    private Gson gson;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCitation(
            @QueryParam("format") @DefaultValue("latex") String format,
            @QueryParam("command") String command,
            @QueryParam("brackets") @DefaultValue("1") int brackets,
            @QueryParam("clipboard") String clipboard,
            @QueryParam("minimize") String minimize,
            @QueryParam("probe") String probe
    ) throws IOException, ExecutionException, InterruptedException {
        if (probe != null && !probe.isEmpty()) {
            return Response.ok("ready").build();
        }

        BibtexImporter bibtexImporter = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        BibDatabaseContext databaseContext;
        try (InputStream chocolateBibInputStream = getChocolateBibAsStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(chocolateBibInputStream, StandardCharsets.UTF_8));
            databaseContext = bibtexImporter.importDatabase(reader).getDatabaseContext();
        }
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

        CompletableFuture<List<BibEntry>> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {

                SearchDialog<BibEntry> dialog = new SearchDialog<>();
                // TODO: Using the DatabaseSearcher directly here results in a lot of exceptions being thrown, so we use an alternative for now until we have a nice way of using the DatabaseSearcher class.
                //  searchDialog.set(new SearchDialog<>(s -> searcher.getMatches(new SearchQuery(s)), entries));
                List<BibEntry> results = dialog.show(s -> {
                    return entries.stream()
                                  .filter(bibEntryCAYWEntry -> matches(bibEntryCAYWEntry, s))
                                  .map(CAYWEntry::getValue)
                                  .toList();
                }, entries);

                future.complete(results);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        List<String> citationKeys = future.get().stream()
                .map(BibEntry::getCitationKey)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        return Response.ok(gson.toJson(citationKeys)).build();
    }

    /// @return a stream to the Chocolate.bib file in the classpath (is null only if the file was moved or there are issues with the classpath)
    private @Nullable InputStream getChocolateBibAsStream() {
        return BibDatabase.class.getResourceAsStream("/Chocolate.bib");
    }

    private CAYWEntry<BibEntry> createCAYWEntry(BibEntry entry) {
        String label = entry.getCitationKey().orElse("");
        String shortLabel = entry.getCitationKey().orElse("");
        String description = entry.getField(StandardField.TITLE).orElse("");
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
