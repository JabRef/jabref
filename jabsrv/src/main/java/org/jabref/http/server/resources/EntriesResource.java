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
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.PlainCitationParserFactory;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.preferences.CliPreferences;
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
    FilesToServe filesToServe;

    @Inject
    Gson gson;

    @Inject
    UiMessageHandler uiMessageHandler;

    /// Appends BibTeX entries to the currently selected library.
    ///
    /// @param group optional name of a group the imported entries are additionally assigned to.
    ///              If the group does not exist, it is created as a top-level group. JabRef merges
    ///              the entries into the library (duplicate handling applies).
    @POST
    @Consumes(MediaTypes.APPLICATION_BIBTEX)
    public void addBibtex(@PathParam("id") String id, @QueryParam("group") @Nullable String group, String bibtex) {
        if (uiMessageHandler == null) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }
        if (bibtex == null || bibtex.isBlank()) {
            throw new BadRequestException("BibTeX data must not be empty.");
        }
        uiMessageHandler.handleUiCommands(List.of(new UiCommand.AppendBibTeXToCurrentLibrary(bibtex, group)));
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
        if (uiMessageHandler == null) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }
        if (citationText == null || citationText.isBlank()) {
            throw new BadRequestException("Citation text must not be empty.");
        }

        PlainCitationParserChoice choice = preferences.getImporterPreferences().getDefaultPlainCitationParser();
        Optional<BibEntry> parsed = parsePlainCitation(choice, citationText);
        if (parsed.isEmpty()) {
            throw new BadRequestException("Could not parse a bibliography entry from the given text.");
        }

        StringWriter rawEntry = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntry, "\n");
        BibEntryWriter entryWriter = new BibEntryWriter(
                new FieldWriter(preferences.getFieldPreferences()),
                new BibEntryTypesManager());
        entryWriter.write(parsed.get(), bibWriter, BibDatabaseMode.BIBTEX);

        uiMessageHandler.handleUiCommands(List.of(new UiCommand.AppendBibTeXToCurrentLibrary(rawEntry.toString(), group)));
    }

    private Optional<BibEntry> parsePlainCitation(PlainCitationParserChoice choice, String citationText) throws FetcherException {
        if (choice == PlainCitationParserChoice.LLM) {
            // The LLM parser needs a ChatModel; build one for this request and
            // close it afterwards so the underlying HTTP client is released.
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

    @POST
    @Consumes("*/*")
    public void addUnknown(@PathParam("id") String id, Reader body) throws IOException {
        if (uiMessageHandler == null) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }

        // Stream is read in another thread - when Grizzly already closed the stream
        // Therefore, we need to create a copy
        java.nio.file.Path tempFile;
        tempFile = Files.createTempFile("JabRef-import", "data");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            body.transferTo(writer);
        }

        uiMessageHandler.handleUiCommands(List.of(new UiCommand.AppendFilesToCurrentLibrary(List.of(tempFile))));
    }

    /// Loops through all entries in the specified library and adds attached files of type "PDF" to
    /// a list and JSON serialises it.
    /// FIXME: JabMap should serve the files per BibEntry. See <https://github.com/JabRef/jabmap/issues/56> for details
    @GET
    @Path("pdffiles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public String getPDFFilesAsList(@PathParam("id") String id) throws IOException {
        // get a list of all entries in library (specified by "id")
        BibDatabaseContext databaseContext = ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
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
