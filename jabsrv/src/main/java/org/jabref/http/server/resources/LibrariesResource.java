package org.jabref.http.server.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.LibraryQueryMatch;
import org.jabref.http.dto.LibraryQueryRequest;
import org.jabref.http.dto.LibraryQueryResponse;
import org.jabref.http.dto.LibraryQueryResult;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import com.google.gson.Gson;
import jakarta.inject.Inject;
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

    /// Runs each query of the request against all open libraries.
    ///
    /// [impl->req~jabsrv.query.search~1]
    /// Queries are processed independently and their results are returned in input
    /// order, so a caller matching a reference list can align the n-th query with
    /// the n-th reference.
    @POST
    @Path("query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LibraryQueryResponse query(LibraryQueryRequest request) {
        List<String> libraryIds = openLibraryIds();

        List<LibraryQueryResult> results = new ArrayList<>();
        for (String rawQuery : request.queries()) {
            results.add(new LibraryQueryResult(rawQuery, runQuery(rawQuery, libraryIds)));
        }
        return new LibraryQueryResponse(results);
    }

    /// Entries without a citation key are reported with the sentinel
    /// [LibraryQueryMatch#UNSET_CITATION_KEY] instead of an empty string, which would
    /// otherwise look indistinguishable from a present-but-empty key. Multiple
    /// unkeyed entries still collide on that sentinel — the API identifies entries by
    /// citation key, so disambiguation requires assigning keys.
    private List<LibraryQueryMatch> runQuery(String rawQuery, List<String> libraryIds) {
        if (StringUtil.isBlank(rawQuery)) {
            return List.of();
        }
        SearchQuery searchQuery = new SearchQuery(rawQuery, EnumSet.noneOf(SearchFlags.class));

        List<LibraryQueryMatch> matches = new ArrayList<>();
        for (String libraryId : libraryIds) {
            try {
                BibDatabaseContext context = ServerUtils.getBibDatabaseContext(libraryId, srvStateManager, preferences.getImportFormatPreferences());
                for (BibEntry entry : runSearch(context, searchQuery)) {
                    matches.add(new LibraryQueryMatch(libraryId, entry.getCitationKey().orElse(LibraryQueryMatch.UNSET_CITATION_KEY)));
                }
            } catch (IOException e) {
                LOGGER.warn("Could not load library {} for query", libraryId, e);
            }
        }
        return matches;
    }

    /// Delegate to whichever [org.jabref.logic.search.SearchContext] the state manager provides. In GUI
    /// mode this is the live orchestrator that may use the Postgres backend; in
    /// stand-alone mode this is a fresh in-memory context.
    private List<BibEntry> runSearch(BibDatabaseContext context, SearchQuery query) {
        SearchResults results = srvStateManager.getSearchContext(context).search(query);
        return context.getDatabase().getEntries().stream()
                      .filter(results::isMatched)
                      .toList();
    }

    private List<String> openLibraryIds() {
        return srvStateManager.getOpenDatabases().stream()
                              .map(BibDatabaseContext::getDatabasePath)
                              .flatMap(java.util.Optional::stream)
                              .map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                              .toList();
    }
}
