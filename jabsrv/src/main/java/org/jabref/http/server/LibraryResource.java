package org.jabref.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.http.JabrefMediaType;
import org.jabref.http.dto.BibEntryDTO;
import org.jabref.http.dto.LinkedPdfFileDTO;
import org.jabref.http.server.services.ContextsToServe;
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
    ContextsToServe contextsToServe;

    @Inject
    FilesToServe filesToServe;

    @Inject
    Gson gson;

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
        if (!Files.exists(jabMapPath)) {
            return """
                    {"map" :
                        {
                            "meta": {
                                "name": "jsMind remote",
                                "author": "hizzgdev@163.com",
                                "version": "0.2"
                            },
                            "format": "node_tree",
                            "data": {
                                "id": "root",
                                "topic": "jsMind",
                                "expanded": true,
                                "children": [
                                    {
                                        "id": "easy",
                                        "topic": "Easy",
                                        "expanded": true,
                                        "direction": "left",
                                        "children": [
                                            {
                                                "id": "easy1",
                                                "topic": "Easy to show",
                                                "expanded": true
                                            },
                                            {
                                                "id": "easy2",
                                                "topic": "Easy to edit",
                                                "expanded": true
                                            },
                                            {
                                                "id": "easy3",
                                                "topic": "Easy to store",
                                                "expanded": true
                                            },
                                            {
                                                "id": "easy4",
                                                "topic": "Easy to embed",
                                                "expanded": true
                                            }
                                        ]
                                    },
                                    {
                                        "id": "open",
                                        "topic": "Open Source",
                                        "expanded": true,
                                        "direction": "right",
                                        "children": [
                                            {
                                                "id": "open1",
                                                "topic": "on GitHub",
                                                "expanded": true
                                            },
                                            {
                                                "id": "open2",
                                                "topic": "BSD License",
                                                "expanded": true
                                            }
                                        ]
                                    }
                                ]
                            }
                        }
                    }""";
        }
        return Files.readString(jabMapPath);
    }

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

        java.nio.file.Path library = ServerUtils.getLibraryPath(id, filesToServe, contextsToServe);
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
        java.nio.file.Path libraryPath = ServerUtils.getLibraryPath(id, filesToServe, contextsToServe);
        String newName = libraryPath.getFileName().toString().replaceFirst("\\.bib$", ".jmp");
        return libraryPath.getParent().resolve(newName);
    }

    private java.nio.file.Path getJabMapDemoPath() {
        java.nio.file.Path result = java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).resolve("demo.jmp");
        // TODO: make this debug - and adapt "tinylog.properties" locally to use debug level
        LOGGER.error("Using temporary file for demo jmp: {}", result);
        return result;
    }

    /// @param id - also "demo" for the Chocolate.bib file
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, contextsToServe, preferences.getImportFormatPreferences());
    }

    /// libraries/{id}/entries/{entryId}
    /* @GET
    @Path("entries/{entryId}")
    @Produces(MediaType.TEXT_HTML)
    public String getPreview(@PathParam("id") String id, @PathParam("entryId") String entryId) throws IOException {
        ParserResult parserResult = getParserResult(id);
        List<BibEntry> entriesByCitationKey = parserResult.getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        if (entriesByCitationKey.size() > 1) {
            LOGGER.warn("Multiple entries found with citation key '{}'. Using the first one.", entryId);
        }
        BibEntry theEntry = entriesByCitationKey.getFirst();

        // TODO: Currently, the preview preferences are in GUI package, which is not accessible here.
        // PreviewLayout layout = preferences.getpr previewPreferences.getSelectedPreviewLayout();
        // return layout.generatePreview(theEntry, parserResult.getDatabaseContext());
        return theEntry.getAuthorTitleYear();
    }*/

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

        // build the preview
        BibEntry entry = entriesByCitationKey.getFirst();

        String author = entry.getField(StandardField.AUTHOR).orElse("(N/A)");
        String title = entry.getField(StandardField.TITLE).orElse("(N/A)");
        String journal = entry.getField(StandardField.JOURNAL).orElse("(N/A)");
        String volume = entry.getField(StandardField.VOLUME).orElse("(N/A)");
        String number = entry.getField(StandardField.NUMBER).orElse("(N/A)");
        String pages = entry.getField(StandardField.PAGES).orElse("(N/A)");
        String releaseDate = entry.getField(StandardField.DATE).orElse("(N/A)");

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

        // build the preview
        BibEntry entry = entriesByCitationKey.getFirst();

        String author = entry.getField(StandardField.AUTHOR).orElse("(N/A)");
        String title = entry.getField(StandardField.TITLE).orElse("(N/A)");
        String journal = entry.getField(StandardField.JOURNAL).orElse("(N/A)");
        String volume = entry.getField(StandardField.VOLUME).orElse("(N/A)");
        String number = entry.getField(StandardField.NUMBER).orElse("(N/A)");
        String pages = entry.getField(StandardField.PAGES).orElse("(N/A)");
        String releaseDate = entry.getField(StandardField.DATE).orElse("(N/A)");

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

    /// libraries/{id}/entries/pdffiles
    // returns a list of all pdf files in the library
    @GET
    @Path("entries/pdffiles")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public String getPDFFilesAsList(@PathParam("id") String id) throws IOException {
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
                    if (!file.getFileType().equals("PDF") || LinkedFile.isOnlineLink(file.getLink())) {
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

    /// @return a stream to the Chocolate.bib file in the classpath (is null only if the file was moved or there are issues with the classpath)
    private @Nullable InputStream getChocolateBibAsStream() {
        return BibDatabase.class.getResourceAsStream("/Chocolate.bib");
    }

    // TODO: write helper function to extract annotations
}
