package org.jabref.http.server.resources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.LinkedPdfFileDTO;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}/entries")
public class EntriesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntriesResource.class);

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    Gson gson;

    @Inject
    UiMessageHandler uiMessageHandler;

    @Inject
    BibEntryTypesManager entryTypesManager;

    /// Appends BibTeX entries to the currently selected library.
    ///
    /// [impl->req~jabsrv.import.group~1]
    ///
    /// @param group optional name of a group the imported entries are additionally assigned to. If the group does not exist, it is created as a top-level group. JabRef merges the entries into the library (duplicate handling applies).
    @POST
    @Consumes(MediaTypes.APPLICATION_BIBTEX)
    public void addBibtex(@PathParam("id") String id, @QueryParam("group") @Nullable String group, String bibtex) {
        if (StringUtil.isBlank(bibtex)) {
            throw new BadRequestException("BibTeX data must not be empty.");
        }
        if (!uiMessageHandler.isGuiConnected()) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        Optional<java.nio.file.Path> targetLibrary = resolveTargetLibrary(id);
        uiMessageHandler.handleUiCommands(List.of(targetLibrary
                .map(library -> new UiCommand.AppendBibTeXToLibrary(library, bibtex, group))
                .orElseGet(() -> new UiCommand.AppendBibTeXToLibrary(bibtex, group))));
    }

    /// Parses a plain-text bibliography reference into a BibTeX entry and appends it to the
    /// currently selected library.
    ///
    /// The reference is run through the plain-citation parser the user selected in JabRef's
    /// preferences ({@link org.jabref.logic.importer.ImporterPreferences#getDefaultPlainCitationParser()}),
    /// including the LLM parser.
    ///
    /// @param group optional name of a group the imported entry is additionally assigned to.
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void addPlainCitation(@PathParam("id") String id, @QueryParam("group") @Nullable String group, String citationText) throws FetcherException, IOException {
        if (StringUtil.isBlank(citationText)) {
            throw new BadRequestException("Citation text must not be empty.");
        }
        if (!uiMessageHandler.isGuiConnected()) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        Optional<java.nio.file.Path> targetLibrary = resolveTargetLibrary(id);

        BibEntry parsed = ServerUtils.parsePlainCitation(preferences, citationText)
                                     .orElseThrow(() -> new BadRequestException("Could not parse a bibliography entry from the given text."));

        StringWriter rawEntry = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntry, "\n");
        BibEntryWriter entryWriter = new BibEntryWriter(
                new FieldWriter(preferences.getFieldPreferences()),
                entryTypesManager);
        entryWriter.write(parsed, bibWriter, BibDatabaseMode.BIBTEX);

        uiMessageHandler.handleUiCommands(List.of(targetLibrary
                .map(library -> new UiCommand.AppendBibTeXToLibrary(library, rawEntry.toString(), group))
                .orElseGet(() -> new UiCommand.AppendBibTeXToLibrary(rawEntry.toString(), group))));
    }

    @POST
    @Consumes("*/*")
    public void addUnknown(@PathParam("id") String id, Reader body) throws IOException {
        if (!uiMessageHandler.isGuiConnected()) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        Optional<java.nio.file.Path> targetLibrary = resolveTargetLibrary(id);

        // Stream is read in another thread - when Grizzly already closed the stream
        // Therefore, we need to create a copy
        java.nio.file.Path tempFile = Files.createTempFile("JabRef-import", "data");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            body.transferTo(writer);
        }

        uiMessageHandler.handleUiCommands(List.of(new UiCommand.AppendFilesToLibrary(targetLibrary, List.of(tempFile))));
    }

    /// Resolves the path-segment library id into the on-disk path the append should target.
    ///
    /// "current" keeps the previous behaviour (empty Optional -> active library). Any other id
    /// is looked up among the open libraries; an unknown or closed id yields 404 via
    /// {@link ServerUtils#getLibraryPath}.
    private Optional<java.nio.file.Path> resolveTargetLibrary(String id) {
        if ("current".equals(id)) {
            return Optional.empty();
        }
        return Optional.of(ServerUtils.getLibraryPath(id, srvStateManager));
    }

    /// Loops through all entries in the specified library and adds attached files of type "PDF" to
    /// a list and JSON serialises it.
    /// FIXME: JabMap should serve the files per BibEntry. See <https://github.com/JabRef/jabmap/issues/56> for details
    @GET
    @Path("pdffiles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public String getPDFFilesAsList(@PathParam("id") String id) throws IOException {
        // get a list of all entries in library (specified by "id")
        BibDatabaseContext databaseContext = ServerUtils.getBibDatabaseContext(id, srvStateManager, preferences.getImportFormatPreferences());
        List<LinkedPdfFileDTO> response = new ArrayList<>();
        List<BibEntry> entries = databaseContext.getDatabase().getEntries();
        if (entries.isEmpty()) {
            throw new NotFoundException("No entries found for library: " + id);
        }

        // loop through all entries to extract pdfs and paths
        for (BibEntry entry : entries) {
            List<LinkedFile> pathsToFiles = entry.getFiles();
            if (!pathsToFiles.isEmpty()) {
                for (LinkedFile file : pathsToFiles) {
                    // ignore all non pdf files and online references
                    if (!"PDF".equals(file.getFileType()) || LinkedFile.isOnlineLink(file.getLink())) {
                        continue;
                    }
                    // add file to response body
                    LinkedPdfFileDTO localPdfFile = new LinkedPdfFileDTO(entry, file);
                    response.add(localPdfFile);
                }
            }
        }
        return gson.toJson(response);
    }
}
