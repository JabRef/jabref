package org.jabref.http.server;

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;

import de.undercouch.citeproc.csl.CSLItemData;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jabref.http.JabrefMediaType;
import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.BibEntryDTO;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.citationstyle.JabRefItemDataProvider;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Path("libraries/{id}/entries/{entryId}")
public class BibEntryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryResource.class);

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    FilesToServe filesToServe;


    /**
     * At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
     *
     * Returns the BibEntry as a CSL JSON object.
     *
     * @param id The name of the library
     * @param entryId The CitationKey of the BibEntry
     * @return JSON representation of the BibEntry
     */
    @GET
    @Produces(JabrefMediaType.JSON_CSL_ITEM)
    public String getCSLJsonRepresentation(@PathParam("id") String id,
                                           @PathParam("entryId") String entryId) throws IOException {
        List<BibEntry> entriesByCitationKey = getDatabaseContext(id)
                .getDatabase()
                .getEntriesByCitationKey(entryId);


        if (entriesByCitationKey.isEmpty()) {
            throw new NotFoundException("Entry with citation key '" + entryId + "' not found in library " + id);
        }
        if (entriesByCitationKey.size() > 1) {
            LOGGER.warn("Multiple Cls json entries found with citation key '{}'. Using the first one.", entryId);
        }
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        JabRefItemDataProvider jabRefItemDataProvider =
                new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(entriesByCitationKey,getDatabaseContext(id),entryTypesManager);

        CSLItemData cslItem = jabRefItemDataProvider.retrieveItem(entryId);


        if (cslItem == null) {
            throw new NotFoundException("Unable to convert entry '" + entryId + "' to CSL JSON");
        }

        Gson gson = new Gson();
        return gson.toJson(List.of(cslItem));
    }

    /**
     * At http://localhost:23119/libraries/{id}/entries/{entryId} <br><br>
     *
     * Returns the BibEntry as a JSON object.
     *
     * @param id The name of the library
     * @param entryId The CitationKey of the BibEntry
     * @return JSON representation of the BibEntry
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJsonRepresentation(@PathParam("id") String id,
                                        @PathParam("entryId") String entryId) throws IOException {
        var databaseContextBib = getDatabaseContext(id);
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        List<BibEntryDTO> list = databaseContextBib.getDatabase().getEntriesByCitationKey(entryId).stream()
                .map(entry -> new BibEntryDTO(entry, databaseContextBib.getMode(), preferences.getFieldPreferences(), entryTypesManager))
                .toList();
        Gson gson = new Gson();

        return gson.toJson(list);
    }


    /**
     * @param id - also "demo" for the Chocolate. bib file
     */
    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }

}
