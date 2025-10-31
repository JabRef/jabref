package org.jabref.http.server;

import java.util.Optional;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.AddEntryDTO;
import org.jabref.http.dto.BibEntryDTO;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.event.EntriesEventSource;

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/latest")
public class LatestLibraryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatestLibraryResource.class);

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private CliPreferences preferences;

    @Inject
    private Gson gson;

    @POST
    @Path("entries")
    public Response addEntry(String jsonInput) {
        // Manual JSON parsing with gson.fromJson
        AddEntryDTO request;
        try {
            if (jsonInput == null || jsonInput.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson(new ErrorResponse("Missing JSON input")))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            request = gson.fromJson(jsonInput, AddEntryDTO.class);
        } catch (JsonSyntaxException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson(new ErrorResponse("Invalid JSON format: " + e.getMessage())))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson(new ErrorResponse("Missing or empty 'text' field")))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Optional<BibDatabaseContext> activeDb = srvStateManager.getActiveDatabase();
        if (activeDb.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson(new ErrorResponse("No active library. Please open a library first.")))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        String bibtexSource = request.getText();

        BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences());

        try {
            Optional<BibEntry> entry = parser.parseSingleEntry(bibtexSource);
            if (entry.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(gson.toJson(new ErrorResponse("No valid BibTeX entry found")))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            activeDb.get().getDatabase().insertEntry(entry.get(), EntriesEventSource.SHARED);

            BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
            BibEntryDTO dto = new BibEntryDTO(entry.get(), activeDb.get().getMode(), preferences.getFieldPreferences(), entryTypesManager);

            // Manual JSON serialization with gson.toJson
            return Response.ok(gson.toJson(dto))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (ParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(gson.toJson(new ErrorResponse("Error parsing BibTeX entry: " + e.getMessage())))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // Helper class for error responses
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
