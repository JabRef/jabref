package org.jabref.http.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import org.jabref.gui.Globals;
import org.jabref.http.JabrefMediaType;
import org.jabref.http.dto.BibEntryDTO;
import org.jabref.logic.citationstyle.JabRefItemDataProvider;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}")
public class LibraryResource {
    public static final Logger LOGGER = LoggerFactory.getLogger(LibraryResource.class);

    @Inject
    PreferencesService preferences;

    @Inject
    Gson gson;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJson(@PathParam("id") String id) {
        ParserResult parserResult = getParserResult(id);
        List<BibEntryDTO> list = parserResult.getDatabase().getEntries().stream()
                                             .peek(bibEntry -> bibEntry.getSharedBibEntryData().setSharedID(Objects.hash(bibEntry)))
                                             .map(entry -> new BibEntryDTO(entry, parserResult.getDatabaseContext().getMode(), preferences.getFieldPreferences(), Globals.entryTypesManager))
                                             .toList();
        return gson.toJson(list);
    }

    @GET
    @Produces(JabrefMediaType.JSON_CSL_ITEM)
    public String getClsItemJson(@PathParam("id") String id) {
        ParserResult parserResult = getParserResult(id);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(parserResult.getDatabaseContext(), new BibEntryTypesManager());
        return jabRefItemDataProvider.toJson();
    }

    private ParserResult getParserResult(String id) {
        java.nio.file.Path library = getLibraryPath(id);
        ParserResult parserResult;
        try {
            parserResult = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor()).importDatabase(library);
        } catch (IOException e) {
            LOGGER.warn("Could not find open library file {}", library, e);
            throw new InternalServerErrorException("Could not parse library", e);
        }
        return parserResult;
    }

    @GET
    @Produces(JabrefMediaType.BIBTEX)
    public Response getBibtex(@PathParam("id") String id) {
        java.nio.file.Path library = getLibraryPath(id);
        String libraryAsString;
        try {
            libraryAsString = Files.readString(library);
        } catch (IOException e) {
            LOGGER.error("Could not read library {}", library, e);
            throw new InternalServerErrorException("Could not read library " + library, e);
        }
        return Response.ok()
                .entity(libraryAsString)
                .build();
    }

    private java.nio.file.Path getLibraryPath(String id) {
        return preferences.getGuiPreferences().getLastFilesOpened()
                          .stream()
                          .filter(p -> (p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p)).equals(id))
                          .findAny()
                          .orElseThrow(NotFoundException::new);
    }
}
