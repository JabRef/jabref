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

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    public Response addEntry(AddEntryDTO request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Missing or empty 'text' field\"}")
                    .build();
        }
        LOGGER.error("HERE 1");

        Optional<BibDatabaseContext> activeDb = srvStateManager.getActiveDatabase();
        if (activeDb.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"No active library. Please open a library first.\"}")
                    .build();
        }
        LOGGER.error("HERE 2");

        String bibtexSource = request.getText();

        LOGGER.error("HERE 3");
        BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences());
        LOGGER.error("HERE 4");
        try {
            LOGGER.error("GUFSF");
            Optional<BibEntry> entry = parser.parseSingleEntry(bibtexSource);
            if (entry.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"No valid BibTeX entry found\"}")
                        .build();
            }
            System.err.println("HERE 5");

            activeDb.get().getDatabase().insertEntry(entry.get());

            BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
            BibEntryDTO dto = new BibEntryDTO(entry.get(), activeDb.get().getMode(), preferences.getFieldPreferences(), entryTypesManager);
            return Response.ok(gson.toJson(dto)).build();
        } catch (ParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Error parsing BibTeX entry: " + e.getMessage().replace("\"", "\\\"") + "\"}")
                    .build();
        }
    }
}
