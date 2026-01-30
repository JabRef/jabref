package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}/entries/{entryId}")
public class EntryResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryResource.class);

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    FilesToServe filesToServe;

    /// At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
    ///
    /// Combines attributes of a given BibEntry into a basic entry preview for as plain text.
    ///
    /// @param id      The name of the library
    /// @param entryId The CitationKey of the BibEntry
    /// @return a basic entry preview as plain text
    @GET
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

    /// At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
    ///
    /// Combines attributes of a given BibEntry into a basic entry preview for as HTML text.
    ///
    /// @param id      The name of the library
    /// @param entryId The CitationKey of the BibEntry
    /// @return a basic entry preview as HTML text
    /// @throws IOException
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

    @POST
    @Path("files")
    @Consumes("application/pdf")
    public void addFile(@PathParam("id") String id, @PathParam("entryId") String entryId, InputStream fileInputStream) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        List<BibEntry> entriesByCitationKey = databaseContext.getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        // 0. Determine BibEntry
        BibEntry entry = entriesByCitationKey.getFirst();

        // 1. Determine target directory
        Optional<java.nio.file.Path> targetDirOpt = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences());
        if (targetDirOpt.isEmpty()) {
            throw new BadRequestException("Library must be saved or have a file directory configured to attach files.");
        }
        java.nio.file.Path targetDir = targetDirOpt.get();

        // 2. Save stream to temporary file
        // We must save to a temp file first because LinkedFileHandler requires an existing Path to generate the suggested filename based on content/metadata.
        // We use the target directory to ensure we are on the same file system.
        java.nio.file.Path tempFile = Files.createTempFile(targetDir, "jabref-upload", ".pdf");
        Files.copy(fileInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        // 3. Create LinkedFile
        LinkedFile linkedFile = new LinkedFile("", tempFile, "PDF");
        LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, preferences.getFilePreferences());

        // 4. Rename to suggested pattern (e.g. Author - Title.pdf)
        boolean renameSuccessful = fileHandler.renameToSuggestedName();
        if (!renameSuccessful) {
            throw new IOException("Failed to rename file to suggested pattern");
        }

        // 5. Add to entry
        entry.addFile(linkedFile);
    }

    /// @param id - also "demo" for the Chocolate.bib file
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }
}
