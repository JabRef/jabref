package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.http.JabrefMediaType;
import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.BibEntryDTO;
import org.jabref.http.dto.LinkedPdfFileDTO;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.citationstyle.JabRefItemDataProvider;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}")
public class LibraryResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryResource.class);

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    FilesToServe filesToServe;

    @Inject
    Gson gson;

    /**
     * At http://localhost:23119/libraries/{id}
     *
     * @param id The specified library
     * @return specified library in JSON format
     * @throws IOException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJson(@PathParam("id") String id) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        List<BibEntryDTO> list = databaseContext.getDatabase().getEntries().stream()
                                                .peek(bibEntry -> bibEntry.getSharedBibEntryData().setSharedID(Objects.hash(bibEntry)))
                                                .map(entry -> new BibEntryDTO(entry, databaseContext.getMode(), preferences.getFieldPreferences(), entryTypesManager))
                                                .toList();
        return gson.toJson(list);
    }

    @GET
    @Produces(JabrefMediaType.JSON_CSL_ITEM)
    public String getClsItemJson(@PathParam("id") String id) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(databaseContext, new BibEntryTypesManager());
        return jabRefItemDataProvider.toJson();
    }

    @GET
    @Produces(JabrefMediaType.BIBTEX)
    public Response getBibtex(@PathParam("id") String id) {
        if ("demo".equals(id)) {
            StreamingOutput stream = output -> {
                try (InputStream in = getChocolateBibAsStream()) {
                    in.transferTo(output);
                }
            };

            return Response.ok(stream)
                           // org.glassfish.jersey.media would be required for a "nice" Java to create ContentDisposition; we avoid this
                           .header("Content-Disposition", "attachment; filename=\"Chocolate.bib\"")
                           .build();
        }

        java.nio.file.Path library = ServerUtils.getLibraryPath(id, filesToServe, srvStateManager);
        String libraryAsString;
        try {
            libraryAsString = Files.readString(library);
        } catch (IOException e) {
            LOGGER.error("Could not read library {}", library, e);
            throw new InternalServerErrorException("Could not read library " + library, e);
        }
        return Response.ok()
                       .header("Content-Disposition", "attachment; filename=\"" + library.getFileName() + "\"")
                       .entity(libraryAsString)
                       .build();
    }

    /// Loops through all entries in the specified library and adds attached files of type "PDF" to
    /// a list and JSON serialises it.
    /// FIXME: JabMap should serve the files per BibEntry. See <https://github.com/JabRef/jabmap/issues/56> for details
    @GET
    @Path("entries/pdffiles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public String getPDFFilesAsList(@PathParam("id") String id) throws IOException {
        // get a list of all entries in library (specified by "id")
        BibDatabaseContext databaseContext = getDatabaseContext(id);
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

    /**
     * @return a stream to the Chocolate.bib file in the classpath (is null only if the file was moved or there are issues with the classpath)
     */
    private @Nullable InputStream getChocolateBibAsStream() {
        return BibDatabase.class.getResourceAsStream("/Chocolate.bib");
    }

    /// @param id - also "demo" for the Chocolate.bib file
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }
}
