package org.jabref.http.server;

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
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
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

    /**
     * At http://localhost:23119/libraries/{id}/map <br><br>
     *
     * Looks for the .jmp file in the directory of the given library ({id}.bib file).
     *
     * @param id The given library
     * @return A JSON String containing the mindmap data. If no {id}.jmp file was found, returns the standard mindmap
     * @throws IOException
     */
    @GET
    @Path("map")
    @Produces(MediaType.APPLICATION_JSON)
    public String getJabMapJson(@PathParam("id") String id) throws IOException {
        boolean isDemo = "demo".equals(id);
        java.nio.file.Path jabMapPath;
        if (isDemo) {
            jabMapPath = getJabMapDemoPath();
        } else {
            jabMapPath = getJabMapPath(id);
        }
        // if no file is found, return the default mindmap
        if (!Files.exists(jabMapPath)) {
            return """
                    {
                      "map": {
                        "meta": {
                          "name": "JabMap",
                          "author": "JabMap",
                          "version": "1.0"
                        },
                        "format": "node_tree",
                        "data": {
                          "id": "root",
                          "topic": "JabMap",
                          "expanded": true,
                          "icons": [],
                          "highlight": null,
                          "type": "Text"
                        }
                      }
                    }
                    """;
        }
        return Files.readString(jabMapPath);
    }

    /**
     * At http://localhost:23119/libraries/{id}/map <br><br>
     *
     * Saves the mindmap next to its associated library.
     *
     * @param id The given library
     *
     * @throws IOException
     */
    @PUT
    @Path("map")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateJabMapJson(@PathParam("id") String id, String fileContent) throws IOException {
        boolean isDemo = "demo".equals(id);
        java.nio.file.Path targetPath;
        if (isDemo) {
            targetPath = getJabMapDemoPath();
        } else {
            targetPath = getJabMapPath(id);
        }
        Files.writeString(targetPath, fileContent);
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

    private java.nio.file.Path getJabMapPath(String id) {
        java.nio.file.Path libraryPath = ServerUtils.getLibraryPath(id, filesToServe, srvStateManager);
        String newName = libraryPath.getFileName().toString().replaceFirst("\\.bib$", ".jmp");
        return libraryPath.getParent().resolve(newName);
    }

    private java.nio.file.Path getJabMapDemoPath() {
        java.nio.file.Path result = java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).resolve("demo.jmp");
        LOGGER.debug("Using temporary file for demo jmp: {}", result);
        return result;
    }

    /**
     * @param id - also "demo" for the Chocolate.bib file
     */
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }

    /**
     * At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
     *
     * Combines attributes of a given BibEntry into a basic entry preview for as plain text.
     *
     * @param id The name of the library
     * @param entryId The CitationKey of the BibEntry
     * @return a basic entry preview as plain text
     * @throws IOException
     * @throws NotFoundException
     */
    @GET
    @Path("entries/{entryId}")
    @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String getPlainRepresentation(@PathParam("id") String id, @PathParam("entryId") String entryId) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        List<BibEntry> entriesByCitationKey = databaseContext.getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        if (entriesByCitationKey.size() > 1) {
            LOGGER.warn("Multiple entries found with citation key '{}'. Using the first one.", entryId);
        }

        // TODO: Currently, the preview preferences are in GUI package, which is not accessible here.
        // build the preview
        BibEntry entry = entriesByCitationKey.getFirst();

        String author = entry.getField(StandardField.AUTHOR).orElse("(N/A)");
        String title = entry.getField(StandardField.TITLE).orElse("(N/A)");
        String journal = entry.getField(StandardField.JOURNAL).orElse("(N/A)");
        String volume = entry.getField(StandardField.VOLUME).orElse("(N/A)");
        String number = entry.getField(StandardField.NUMBER).orElse("(N/A)");
        String pages = entry.getField(StandardField.PAGES).orElse("(N/A)");
        String releaseDate = entry.getField(StandardField.DATE).orElse("(N/A)");

        // the only difference to the HTML version of this method is the format of the output:
        String preview =
                "Author: " + author
                        + "\nTitle: " + title
                        + "\nJournal: " + journal
                        + "\nVolume: " + volume
                        + "\nNumber: " + number
                        + "\nPages: " + pages
                        + "\nReleased on: " + releaseDate;

        return preview;
    }

    /**
     * At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
     *
     * Combines attributes of a given BibEntry into a basic entry preview for as HTML text.
     *
     * @param id The name of the library
     * @param entryId The CitationKey of the BibEntry
     * @return a basic entry preview as HTML text
     * @throws IOException
     */
    @GET
    @Path("entries/{entryId}")
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public String getHTMLRepresentation(@PathParam("id") String id, @PathParam("entryId") String entryId) throws IOException {
        List<BibEntry> entriesByCitationKey = getDatabaseContext(id).getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        if (entriesByCitationKey.size() > 1) {
            LOGGER.warn("Multiple entries found with citation key '{}'. Using the first one.", entryId);
        }

        // TODO: Currently, the preview preferences are in GUI package, which is not accessible here.
        // build the preview
        BibEntry entry = entriesByCitationKey.getFirst();

        String author = entry.getField(StandardField.AUTHOR).orElse("(N/A)");
        String title = entry.getField(StandardField.TITLE).orElse("(N/A)");
        String journal = entry.getField(StandardField.JOURNAL).orElse("(N/A)");
        String volume = entry.getField(StandardField.VOLUME).orElse("(N/A)");
        String number = entry.getField(StandardField.NUMBER).orElse("(N/A)");
        String pages = entry.getField(StandardField.PAGES).orElse("(N/A)");
        String releaseDate = entry.getField(StandardField.DATE).orElse("(N/A)");

        // the only difference to the plain text version of this method is the format of the output:
        String preview =
                "<strong>Author:</strong> " + author + "<br>" +
                        "<strong>Title:</strong> " + title + "<br>" +
                        "<strong>Journal:</strong> " + journal + "<br>" +
                        "<strong>Volume:</strong> " + volume + "<br>" +
                        "<strong>Number:</strong> " + number + "<br>" +
                        "<strong>Pages:</strong> " + pages + "<br>" +
                        "<strong>Released on:</strong> " + releaseDate;

        return preview;
    }

    /**
     * At http://localhost:23119/libraries/{id}/entries/pdffiles <br><br>
     *
     * Loops through all entries in the specified library and adds attached files of type "PDF" to
     * a list and JSON serialises it.
     */
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
}
