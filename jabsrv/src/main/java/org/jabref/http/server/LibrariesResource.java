package org.jabref.http.server;

import java.util.ArrayList;
import java.util.List;

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
    private FilesToServe filesToServe;

    @Inject
    private Gson gson;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<String> fileNamesWithUniqueSuffix = filesToServe.getFilesToServe().stream()
                                                            .map(p -> p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p))
                                                            .toList();
        List<String> result = new ArrayList<>(fileNamesWithUniqueSuffix);
        result.add("demo");
        return gson.toJson(result);
    }
}
