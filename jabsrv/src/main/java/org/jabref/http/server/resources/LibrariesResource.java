package org.jabref.http.server.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.LibraryQueryMatch;
import org.jabref.http.dto.LibraryQueryRequest;
import org.jabref.http.dto.LibraryQueryResponse;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.LibraryQueryExpressionBuilder;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.inmemory.InMemoryLibrarySearcher;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries")
public class LibrariesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibrariesResource.class);

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private Gson gson;

    @Inject
    private CliPreferences preferences;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<String> result = new ArrayList<>(openLibraryIds());
        result.add("demo");
        return gson.toJson(result);
    }

    @POST
    @Path("query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String query(String requestBody) {
        // [impl->req~jabsrv.query.doi~1]
        // [impl->req~jabsrv.query.url~1]
        LibraryQueryRequest request = gson.fromJson(requestBody, LibraryQueryRequest.class);
        if (request == null) {
            throw new BadRequestException("Request body must not be empty");
        }

        List<String> normalizedDois = request.dois().stream()
                                             .flatMap(raw -> DOI.parse(raw).stream())
                                             .map(doi -> doi.asString().toLowerCase(Locale.ROOT))
                                             .toList();

        Optional<String> expression = LibraryQueryExpressionBuilder.build(normalizedDois, request.urls());
        if (expression.isEmpty()) {
            return gson.toJson(new LibraryQueryResponse(List.of()));
        }

        SearchQuery searchQuery = new SearchQuery(expression.get(), EnumSet.noneOf(SearchFlags.class));

        List<LibraryQueryMatch> matches = new ArrayList<>();
        for (String libraryId : openLibraryIds()) {
            try {
                BibDatabaseContext context = ServerUtils.getBibDatabaseContext(libraryId, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
                List<BibEntry> hits = new InMemoryLibrarySearcher(context).getMatches(searchQuery);
                attributeMatches(hits, libraryId, normalizedDois, request.urls(), matches);
            } catch (IOException e) {
                LOGGER.warn("Could not load library {} for query", libraryId, e);
            }
        }

        return gson.toJson(new LibraryQueryResponse(matches));
    }

    private List<String> openLibraryIds() {
        Stream<java.nio.file.Path> pathStream;
        if (!filesToServe.isEmpty()) {
            pathStream = filesToServe.getFilesToServe().stream();
        } else {
            pathStream = srvStateManager.getOpenDatabases().stream()
                                        .filter(context -> context.getDatabasePath().isPresent())
                                        .map(context -> context.getDatabasePath().get());
        }
        return pathStream.map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                         .toList();
    }

    /// The searcher returns matching entries but not which input DOI/URL matched.
    /// Re-derive that attribution by comparing each matched entry's DOI/URL field
    /// against the normalized input lists.
    private void attributeMatches(List<BibEntry> hits, String libraryId, List<String> normalizedDois, List<String> urls, List<LibraryQueryMatch> matches) {
        for (BibEntry entry : hits) {
            String entryId = entry.getCitationKey().orElse("");
            if (!normalizedDois.isEmpty()) {
                entry.getDOI()
                     .map(doi -> doi.asString().toLowerCase(Locale.ROOT))
                     .filter(normalizedDois::contains)
                     .ifPresent(doi -> matches.add(LibraryQueryMatch.forDoi(doi, libraryId, entryId)));
            }
            if (!urls.isEmpty()) {
                entry.getField(StandardField.URL)
                     .filter(urls::contains)
                     .ifPresent(url -> matches.add(LibraryQueryMatch.forUrl(url, libraryId, entryId)));
            }
        }
    }
}
