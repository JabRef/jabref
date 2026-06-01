package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.CitationCacheService;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.PlainCitationParserFactory;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
public class CitationsResource {

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    Gson gson;

    @Inject
    @Nullable
    UiMessageHandler uiMessageHandler;

    @Inject
    CitationCacheService citationCacheService;

    public record LookupMatch(String entryId) {
    }

    public record LookupResponse(List<LookupMatch> matches, String parserCacheKey, String parsedEntryType) {
    }

    @POST
    @Path("lookup")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public LookupResponse lookup(@PathParam("id") String id, String citationText) throws FetcherException {
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }
        if (StringUtil.isBlank(citationText)) {
            throw new BadRequestException("Citation text must not be empty.");
        }

        PlainCitationParserChoice choice = preferences.getImporterPreferences().getDefaultPlainCitationParser();
        BibEntry parsed = parsePlainCitation(choice, citationText)
                .orElseThrow(() -> new BadRequestException("Could not parse a bibliography entry from the given text."));

        BibDatabaseContext context = srvStateManager.getActiveDatabase()
                .orElseThrow(() -> new BadRequestException("No active library"));
        BibDatabaseMode mode = context.getMode();
        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
        List<LookupMatch> matches = duplicateCheck.containsDuplicate(context.getDatabase(), parsed, mode)
                                                  .map(existing -> List.of(new LookupMatch(existing.getCitationKey().orElse(""))))
                                                  .orElse(List.of());

        String cacheKey = citationCacheService.put(parsed, citationText);
        return new LookupResponse(matches, cacheKey, parsed.getType().getName());
    }

    @POST
    @Path("{parserCacheKey}")
    public Response addFromCache(@PathParam("id") String id,
                                 @PathParam("parserCacheKey") String parserCacheKey,
                                 @QueryParam("group") @Nullable String group) throws IOException {
        if (uiMessageHandler == null) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }

        Optional<CitationCacheService.CachedCitation> cached = citationCacheService.get(parserCacheKey);
        if (cached.isEmpty()) {
            // Distinct from 404: the token was never issued OR (more commonly)
            // its TTL expired. 410 Gone tells the client to re-do the lookup
            // and pay the parser cost again.
            throw new ClientErrorException("Cached citation expired or unknown — re-run /citations/lookup",
                                           Response.Status.GONE);
        }

        BibEntry parsed = cached.get().parsed();
        StringWriter rawEntry = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntry, "\n");
        BibEntryWriter entryWriter = new BibEntryWriter(
                new FieldWriter(preferences.getFieldPreferences()),
                new BibEntryTypesManager());
        entryWriter.write(parsed, bibWriter, BibDatabaseMode.BIBTEX);

        uiMessageHandler.handleUiCommands(List.of(group == null
                                                  ? new UiCommand.AppendBibTeXToCurrentLibrary(rawEntry.toString())
                                                  : new UiCommand.AppendBibTeXToCurrentLibrary(rawEntry.toString(), group)));

        // Evict so the same token can't add a second copy. The client
        // already has confirmation it landed; re-tries fall through to
        // the 410 branch above and trigger a fresh lookup.
        citationCacheService.invalidate(parserCacheKey);
        return Response.status(Response.Status.CREATED).build();
    }

    /// Mirrors `EntriesResource.parsePlainCitation` — kept in sync by hand for
    /// now; pulling it into a shared helper is a follow-up refactor.
    private Optional<BibEntry> parsePlainCitation(PlainCitationParserChoice choice, String citationText) throws FetcherException {
        if (choice == PlainCitationParserChoice.LLM) {
            try (ChatModel chatModel = ChatModelFactory.create(preferences.getAiPreferences())) {
                return PlainCitationParserFactory.getLlmPlainCitationParser(
                                                         preferences.getImportFormatPreferences(),
                                                         preferences.getAiPreferences(),
                                                         chatModel)
                                                 .parsePlainCitation(citationText);
            }
        }
        return PlainCitationParserFactory.getPlainCitationParser(
                choice,
                preferences.getCitationKeyPatternPreferences(),
                preferences.getGrobidPreferences(),
                preferences.getImportFormatPreferences()).parsePlainCitation(citationText);
    }
}
