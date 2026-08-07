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
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Custom-action endpoint that runs each query of the request against
/// all open libraries.
///
/// The URL form `libraries:query` follows Google's
/// [API Improvement Proposal 136 — Custom methods](https://google.aip.dev/136):
/// custom (non-standard CRUD) methods are spelled `<resource>:<verb>` with a
/// literal colon and no separating slash. Quoting AIP-136:
///
/// > Custom methods should use HTTP `POST` if they have side effects, or HTTP
/// > `GET` if they are side-effect free. ... Custom methods should generally
/// > take the form: `https://service.googleapis.com/v1/resources/{id}:verb`.
/// > ... For batch and collection-level custom methods, the colon is appended
/// > directly to the collection name: `https://.../v1/resources:batchGet`.
///
/// Practical reason for the colon here: the sibling
/// [LibraryResource] is mounted at `@Path("libraries/{id}")`. A
/// `libraries/query` sibling would lose JAX-RS path-specificity
/// arbitration because the per-library path matches `{id}=query` with
/// more literal characters — the request would dispatch to
/// [LibraryResource] and return 405. The colon variant has no slash
/// and therefore no collision.
@Path("libraries:query")
public class LibrariesQueryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibrariesQueryResource.class);

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private CliPreferences preferences;

    /// [impl->req~jabsrv.query.search~1]
    /// Queries are processed independently and their results are returned in input
    /// order, so a caller matching a reference list can align the n-th query with
    /// the n-th reference.
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LibraryQueryResponse query(LibraryQueryRequest request) {
        List<String> libraryIds = ServerUtils.openLibraryIds(srvStateManager);

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
}
