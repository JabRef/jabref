package org.jabref.http.server.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.logic.util.io.BackupFileUtil;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("libraries")
public class LibrariesResource {

    @Inject
    private SrvStateManager srvStateManager;

    @Inject
    private FilesToServe filesToServe;

    @Inject
    private Gson gson;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        Stream<java.nio.file.Path> pathStream;
        if (!filesToServe.isEmpty()) {
            pathStream = filesToServe.getFilesToServe().stream();
        } else {
            pathStream = srvStateManager.getOpenDatabases().stream()
                                        .filter(context -> context.getDatabasePath().isPresent())
                                        .map(context -> context.getDatabasePath().get());
        }
        List<String> fileNamesWithUniqueSuffix = pathStream.map(path -> path.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(path))
                                                           .toList();
        List<String> result = new ArrayList<>(fileNamesWithUniqueSuffix);
        result.add("demo");
        return gson.toJson(result);
    }
}
