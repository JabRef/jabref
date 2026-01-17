package org.jabref.http.server.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.LinkedPdfFileDTO;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
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
import jakarta.ws.rs.core.MediaType;

@Path("libraries/{id}/entries")
public class EntriesResource {

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

    @POST
    @Consumes(MediaTypes.APPLICATION_BIBTEX)
    public void addBibtex(@PathParam("id") String id, String bibtex) {
        if (uiMessageHandler == null) {
            throw new BadRequestException("Only possible in GUI mode.");
        }
        if (!"current".equals(id)) {
            throw new BadRequestException("Only currently selected library possible");
        }
        if (bibtex == null || bibtex.isBlank()) {
            throw new BadRequestException("BibTeX data must not be empty.");
        }
        uiMessageHandler.handleUiCommands(List.of(new UiCommand.AppendBibTeXToCurrentLibrary(bibtex)));
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
