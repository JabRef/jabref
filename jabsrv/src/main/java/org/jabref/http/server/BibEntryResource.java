package org.jabref.http.server;

import java.io.IOException;
import java.util.List;

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

import com.airhacks.afterburner.injection.Injector;
import com.google.gson.Gson;
import de.undercouch.citeproc.csl.CSLItemData;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}/entries/{entryId}")
public class BibEntryResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryResource.class);

    @Inject
    private CliPreferences preferences;

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private Gson gson;

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
            LOGGER.warn("Multiple CSL JSON entries found with citation key '{}'. Using the first one.", entryId);
        }

        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(entriesByCitationKey, getDatabaseContext(id), entryTypesManager);

        CSLItemData cslItem = jabRefItemDataProvider.retrieveItem(entryId);

        if (cslItem == null) {
            throw new NotFoundException("Unable to convert entry '" + entryId + "' to CSL JSON");
        }

        return gson.toJson(List.of(cslItem));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJsonRepresentation(@PathParam("id") String id,
                                        @PathParam("entryId") String entryId) throws IOException {
        BibDatabaseContext databaseContextBib = getDatabaseContext(id);
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);

        List<BibEntryDTO> list = databaseContextBib.getDatabase()
                .getEntriesByCitationKey(entryId)
                .stream()
                .map(entry -> new BibEntryDTO(entry,
                        databaseContextBib.getMode(),
                        preferences.getFieldPreferences(),
                        entryTypesManager))
                .toList();

        return gson.toJson(list);
    }

    private BibDatabaseContext getDatabaseContext(String id) throws IOException {
        return ServerUtils.getBibDatabaseContext(id, filesToServe, srvStateManager, preferences.getImportFormatPreferences());
    }
}
