package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.CitationCacheService;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Cached plain-citation lookup + later add.
///
/// Two endpoints designed for the hover-then-Ctrl+J SumatraPDF flow (the LLM
/// parser is expensive; we don't want to pay twice for "is it already in the
/// library?" and "add it"):
///
/// 1. `POST /libraries/{id}/citations/lookup` — parse plain citation text,
///    run [DuplicateCheck] against the active library, stash the parsed
///    [BibEntry] in [CitationCacheService], return matches plus the cache
///    token.
/// 2. `POST /libraries/{id}/citations/{parserCacheKey}` — look up the cached
///    parsed entry by token, append it to the current library, evict the
///    token. Returns 410 Gone when the token expired so the client can fall
///    back to `POST /libraries/current/entries` and pay for the parse again.
@Path("libraries/{id}/citations")
@NullMarked
public class CitationsResource {

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    UiMessageHandler uiMessageHandler;

    @Inject
    CitationCacheService citationCacheService;

    public record LookupMatch(String libraryId, String entryId, boolean inActiveLibrary) {
    }

    /// Categorisation of a [LookupResponse]'s `matches` list. Surfaced as a
    /// dedicated field so clients can colour the hover badge without walking
    /// the array themselves.
    public enum MatchScope {
        /// At least one match in the currently-active library — Ctrl+J would
        /// create a duplicate. Mint badge.
        ACTIVE("active"),
        /// Match(es) only in *other* open libraries — Ctrl+J would still
        /// create a new entry in the active library. Olive badge.
        OTHER("other"),
        /// No match in any open library. Gray badge.
        NONE("none");

        private final String jsonValue;

        MatchScope(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        @JsonValue
        public String jsonValue() {
            return jsonValue;
        }
    }

    /// Result of a single plain-citation lookup.
    ///
    /// @param matches duplicate hits across every open library. Empty list when no library contains the parsed entry, or when the parse itself failed (batch-lookup blank-slot / parser-failure rows).
    /// @param matchScope categorisation of `matches` — see [MatchScope].
    /// @param parserCacheKey opaque token redeemable at `POST /libraries/{id}/citations/{parserCacheKey}` to append the already-parsed `BibEntry` without paying for the LLM parse again. Null when no entry was parsed.
    /// @param parsedEntryType BibTeX entry-type name of the parsed entry (e.g. `"article"`, `"inproceedings"`) — lets the client preview the type before redeeming the cache key. Null when no entry was parsed.
    public record LookupResponse(List<LookupMatch> matches, MatchScope matchScope,
                                 @Nullable String parserCacheKey, @Nullable String parsedEntryType) {
    }

    public record AlreadyExistsResponse(String status, String entryId) {
        public AlreadyExistsResponse(String entryId) {
            this("already-exists", entryId);
        }
    }

    @POST
    @Path("lookup")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LookupResponse lookup(@PathParam("id") String id, String citationText) throws FetcherException {
        if (StringUtil.isBlank(citationText)) {
            throw new BadRequestException("Citation text must not be empty.");
        }
        // The target library is the one Ctrl+J would add to: "current" -> active
        // library, any other id -> that open library (404 if unknown/closed).
        BibDatabaseContext targetContext = resolveTargetContext(id);
        return doLookup(citationText, targetContext,
                        srvStateManager.getOpenDatabases(),
                        new DuplicateCheck(new BibEntryTypesManager()));
    }

    /// Wrapper for the batched lookup payload. Plain `List<String>` would work
    /// too, but a named record makes the JSON self-describing and leaves room
    /// to add per-request options (e.g. include-snippets) without breaking the
    /// schema.
    public record BatchLookupRequest(List<String> citations) {
    }

    /// Aligned-by-index with the request: `results[i]` corresponds to
    /// `citations[i]`. Blank input slots yield an empty `LookupResponse`
    /// (matches=[], matchScope="none") rather than failing the whole batch.
    public record BatchLookupResponse(List<LookupResponse> results) {
    }

    /// Batched variant of [#lookup] for the SumatraPDF page-scan flow: when
    /// the reader lands on a new page we extract every citation link's
    /// extracted bibliography text in one shot, fire one POST, and paint the
    /// returned per-citation `matchScope` as in-PDF dots. Single round-trip
    /// + JabRef-side [CitationCacheService#getByText] hits mean repeat
    /// scans of the same page are O(1) per citation with no LLM cost.
    @POST
    @Path("lookup:batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BatchLookupResponse lookupBatch(@PathParam("id") String id, BatchLookupRequest request) throws FetcherException {
        if (request == null || request.citations() == null || request.citations().isEmpty()) {
            throw new BadRequestException("Citations list must not be empty.");
        }
        BibDatabaseContext targetContext = resolveTargetContext(id);
        List<BibDatabaseContext> openDatabases = srvStateManager.getOpenDatabases();
        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());

        List<LookupResponse> results = new ArrayList<>(request.citations().size());
        for (String citationText : request.citations()) {
            if (StringUtil.isBlank(citationText)) {
                // Empty slot — return a "none" response so client indexing
                // stays aligned. Skipping would shift downstream results.
                results.add(new LookupResponse(List.of(), MatchScope.NONE, null, null));
                continue;
            }
            try {
                results.add(doLookup(citationText, targetContext, openDatabases, duplicateCheck));
            } catch (FetcherException e) {
                // One citation failing to parse shouldn't fail the batch.
                // Surface as an empty response so the client paints "no match"
                // (= gray ring) for that slot; the others still resolve.
                results.add(new LookupResponse(List.of(), MatchScope.NONE, null, null));
            }
        }
        return new BatchLookupResponse(results);
    }

    /// Shared body of single + batched lookup. Resolves the citation text to
    /// a `BibEntry` (via the text-hash cache when possible, else the
    /// LLM/fetcher), walks every open library for duplicates, mints a fresh
    /// token for the add-from-cache flow, and assembles the response.
    private LookupResponse doLookup(String citationText,
                                    BibDatabaseContext targetContext,
                                    List<BibDatabaseContext> openDatabases,
                                    DuplicateCheck duplicateCheck) throws FetcherException {
        // Text cache first: lets a re-scan of the same PDF page (or two
        // distinct PDFs that cite the same paper) reuse the prior parse and
        // skip the LLM call entirely.
        BibEntry parsed = citationCacheService.getByText(citationText)
                                              .map(CitationCacheService.CachedCitation::parsed)
                                              .orElse(null);
        if (parsed == null) {
            parsed = ServerUtils.parsePlainCitation(preferences, citationText)
                                 .orElseThrow(() -> new BadRequestException("Could not parse a bibliography entry from the given text."));
            citationCacheService.putByText(parsed, citationText);
        }

        // Cross-library lookup: walk every open library so we can tell the
        // client whether the citation is in the *target* library (Ctrl+J
        // would create a duplicate → mint) vs an *other* open library (still
        // worth surfacing — the user may want to switch libraries or copy
        // across → olive, AnchorHub's related-match colour).
        List<LookupMatch> matches = new ArrayList<>();
        for (BibDatabaseContext ctx : openDatabases) {
            boolean isActive = ctx == targetContext;
            duplicateCheck.containsDuplicate(ctx.getDatabase(), parsed, ctx.getMode())
                          .ifPresent(existing -> matches.add(new LookupMatch(
                                  libraryIdFor(ctx),
                                  existing.getCitationKey().orElse(""),
                                  isActive)));
        }
        MatchScope matchScope = matches.stream()
                                       .anyMatch(LookupMatch::inActiveLibrary) ? MatchScope.ACTIVE
                                                                               : matches.isEmpty() ? MatchScope.NONE : MatchScope.OTHER;

        String cacheKey = citationCacheService.put(parsed, citationText);
        return new LookupResponse(matches, matchScope, cacheKey, parsed.getType().getName());
    }

    /// Stable-ish library identifier for response bodies. Saved libraries use
    /// the absolute path; unsaved libraries fall back to an in-process id
    /// derived from the BibDatabaseContext so the client at least sees they
    /// are distinct.
    private static String libraryIdFor(BibDatabaseContext ctx) {
        return ctx.getDatabasePath()
                  .map(p -> p.toAbsolutePath().toString())
                  .orElseGet(() -> "unsaved-" + Integer.toHexString(System.identityHashCode(ctx)));
    }

    @POST
    @Path("{parserCacheKey}")
    public Response addFromCache(@PathParam("id") String id,
                                 @PathParam("parserCacheKey") String parserCacheKey,
                                 @QueryParam("group") @Nullable String group) {
        if (!uiMessageHandler.isGuiConnected()) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        BibDatabaseContext context = resolveTargetContext(id);
        Optional<java.nio.file.Path> targetLibrary = targetLibrary(id);

        Optional<CitationCacheService.CachedCitation> cached = citationCacheService.get(parserCacheKey);
        if (cached.isEmpty()) {
            // Distinct from 404: the token was never issued OR (more commonly)
            // its TTL expired. 410 Gone tells the client to re-do the lookup
            // and pay the parser cost again.
            throw new ClientErrorException("Cached citation expired or unknown — re-run /citations/lookup",
                    Response.Status.GONE);
        }

        BibEntry parsed = cached.get().parsed();

        // Defense-in-depth duplicate check: even when SumatraPDF's local
        // pushed-set is cold (cleared session, different machine), the
        // server refuses to append a second copy of an already-present
        // citation. Returns 200 with `{"status":"already-exists"}` instead
        // of 201 so the client can show a "Citation is already in library"
        // notification without treating it as a hard error.
        BibDatabaseMode mode = context.getMode();
        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
        return duplicateCheck.containsDuplicate(context.getDatabase(), parsed, mode)
                             .map(existingEntry -> alreadyExistsResponse(parserCacheKey, existingEntry))
                             .orElseGet(() -> appendParsedAndRespond(parsed, targetLibrary, group, parserCacheKey));
    }

    /// Returns a 200 OK `{"status":"already-exists","entryId":"…"}` body and
    /// evicts the cache token so a retry can't double-add the entry.
    private Response alreadyExistsResponse(String parserCacheKey, BibEntry existingEntry) {
        citationCacheService.invalidate(parserCacheKey);
        // Prefer the existing entry's citation key; fall back to a short
        // author/title/year preview when the key is missing or empty.
        String entryId = existingEntry.getCitationKey()
                                      .filter(k -> !k.isBlank())
                                      .orElseGet(() -> existingEntry.getAuthorTitleYear(20));
        return Response.ok(new AlreadyExistsResponse(entryId))
                       .type(MediaType.APPLICATION_JSON)
                       .build();
    }

    /// Writes the parsed entry as BibTeX, forwards it to the UI for append,
    /// evicts the cache token, and returns 201 Created.
    private Response appendParsedAndRespond(BibEntry parsed, Optional<java.nio.file.Path> targetLibrary, @Nullable String group, String parserCacheKey) {
        StringWriter rawEntry = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntry, "\n");
        BibEntryWriter entryWriter = new BibEntryWriter(
                new FieldWriter(preferences.getFieldPreferences()),
                new BibEntryTypesManager());
        try {
            entryWriter.write(parsed, bibWriter, BibDatabaseMode.BIBTEX);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to serialise BibEntry", e);
        }

        // uiMessageHandler is null-checked at the top of addFromCache; safe here.
        uiMessageHandler.handleUiCommands(List.of(targetLibrary
                .map(library -> new UiCommand.AppendBibTeXToLibrary(library, rawEntry.toString(), group))
                .orElseGet(() -> new UiCommand.AppendBibTeXToLibrary(rawEntry.toString(), group))));

        // Evict so the same token can't add a second copy. The client
        // already has confirmation it landed; re-tries fall through to
        // the 410 branch above and trigger a fresh lookup.
        citationCacheService.invalidate(parserCacheKey);
        return Response.status(Response.Status.CREATED).build();
    }

    /// Resolves the path-segment library id to the library this request operates on.
    /// "current" -> active library; any other id -> that open library (404 if unknown or
    /// closed). Same id semantics as {@link EntriesResource}.
    private BibDatabaseContext resolveTargetContext(String id) {
        try {
            return ServerUtils.getBibDatabaseContext(id, srvStateManager, preferences.getImportFormatPreferences());
        } catch (IOException e) {
            throw new InternalServerErrorException("Could not load library " + id, e);
        }
    }

    /// The append target for the resolved library: empty for "current" (append to the active
    /// tab without switching), otherwise the open library's on-disk path so the GUI switches to it
    /// first. Mirrors {@link EntriesResource#resolveTargetLibrary}.
    ///
    /// Non-"current" ids are resolved against the *open* libraries via [ServerUtils#getLibraryPath]
    /// rather than the resolved context's path. This rejects path-less contexts such as the bundled
    /// "demo" library with 404 instead of silently falling back to an empty Optional (which
    /// [UiCommand.AppendBibTeXToLibrary] would interpret as "append to the active library").
    private Optional<java.nio.file.Path> targetLibrary(String id) {
        if ("current".equals(id)) {
            return Optional.empty();
        }
        return Optional.of(ServerUtils.getLibraryPath(id, srvStateManager));
    }
}
