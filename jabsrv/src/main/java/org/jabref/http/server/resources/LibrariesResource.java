package org.jabref.http.server.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.BackupFileUtil;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries")
public class LibrariesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibrariesResource.class);

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private Gson gson;

    @Inject
    private CliPreferences preferences;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<String> result = new ArrayList<>(openLibraryIds());
        result.add("demo");
        return gson.toJson(result);
    }

    private List<String> openLibraryIds() {
        Stream<java.nio.file.Path> pathStream;
        if (!filesToServe.isEmpty()) {
            pathStream = filesToServe.getFilesToServe().stream();
        } else {
            pathStream = srvStateManager.getOpenDatabases().stream()
                                        .filter(context -> context.getDatabasePath().isPresent())
                                        .map(context -> context.getDatabasePath().get());
        }
        return pathStream.map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                         .toList();
    }
}
