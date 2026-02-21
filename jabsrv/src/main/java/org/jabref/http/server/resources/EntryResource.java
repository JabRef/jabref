package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.PreviewPreferences;
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
import jakarta.ws.rs.core.Response;
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

    @Inject
    PreviewPreferences previewPreferences;

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
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public String getHTMLRepresentation(@PathParam("id") String id, @PathParam("entryId") String entryId) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        List<BibEntry> entriesByCitationKey = databaseContext.getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        if (entriesByCitationKey.size() > 1) {
            LOGGER.warn("Multiple entries found with citation key '{}'. Using the first one.", entryId);
        }

        BibEntry entry = entriesByCitationKey.getFirst();
        PreviewLayout previewLayout = previewPreferences.getSelectedPreviewLayout();
        return previewLayout.generatePreview(entry, databaseContext);
    }

    @POST
    @Path("files")
    @Consumes("application/pdf")
    public Response addFile(@PathParam("id") String id, @PathParam("entryId") String entryId, InputStream fileInputStream) throws IOException {
        BibDatabaseContext databaseContext = getDatabaseContext(id);
        List<BibEntry> entriesByCitationKey = databaseContext.getDatabase().getEntriesByCitationKey(entryId);
        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }

        // 1. Determine BibEntry
        BibEntry entry = entriesByCitationKey.getFirst();

        // 2. Determine target directory
        Optional<java.nio.file.Path> targetDirOpt = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences());

        if (targetDirOpt.isEmpty()) {
            throw new BadRequestException("Library must be saved or have a file directory configured to attach files.");
        }
        java.nio.file.Path targetDir = targetDirOpt.get();

        // 3. Save stream to temporary file
        String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        java.nio.file.Path tempFile = Files.createTempFile(targetDir, "jabref-upload-" + timestamp + "-", ".pdf");
        Files.copy(fileInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        // 4. Create LinkedFile
        LinkedFile linkedFile = new LinkedFile(tempFile.getFileName().toString(), tempFile, "PDF");

        // 5. Rename to suggested pattern (e.g. Author - Title.pdf)
        LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, preferences.getFilePreferences());
        boolean renameSuccessful = fileHandler.renameToSuggestedName();
        if (!renameSuccessful) {
            LOGGER.warn("Renaming to suggested name failed. Keeping temp filename.");
        }

        // 6. Add to entry
        entry.addFile(linkedFile);

        if (renameSuccessful) {
            return Response.noContent().build();
        } else {
            return Response.ok("File uploaded but could not be renamed to suggested pattern.").build();
        }
    }

    /// @param id - also "demo" for the Chocolate.bib file
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }
}
