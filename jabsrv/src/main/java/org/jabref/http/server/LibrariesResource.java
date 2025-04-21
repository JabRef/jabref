package org.jabref.http.server;

import java.util.List;

import org.jabref.logic.preferences.CliPreferences;
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
    CliPreferences preferences;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        List<String> fileNamesWithUniqueSuffix = preferences.getLastFilesOpenedPreferences().getLastFilesOpened().stream()
                                                            .map(p -> p.getFileName() + "-" + BackupFileUtil.getUniqueFilePrefix(p))
                                                            .toList();
        return new Gson().toJson(fileNamesWithUniqueSuffix);
    }
}
