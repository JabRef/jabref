package org.jabref.http.server.resources;

import java.io.IOException;
import java.nio.file.Files;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.http.server.services.ServerUtils;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("libraries/{id}/map")
public class MapResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapResource.class);

    @Inject
    SrvStateManager srvStateManager;

    @Inject
    FilesToServe filesToServe;

    /**
     * At http://localhost:23119/libraries/{id}/map <br><br>
     * <p>
     * Looks for the .jmp file in the directory of the given library ({id}.bib file).
     *
     * @param id The given library
     * @return A JSON String containing the mindmap data. If no {id}.jmp file was found, returns the standard mindmap
     * @throws IOException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJabMapJson(@PathParam("id") String id) throws IOException {
        boolean isDemo = "demo".equals(id);
        java.nio.file.Path jabMapPath;
        if (isDemo) {
            jabMapPath = getJabMapDemoPath();
        } else {
            jabMapPath = getJabMapPath(id);
        }
        // if no file is found, return the default mindmap
        if (!Files.exists(jabMapPath)) {
            return """
                    {
                      "map": {
                        "meta": {
                          "name": "JabMap",
                          "author": "JabMap",
                          "version": "1.0"
                        },
                        "format": "node_tree",
                        "data": {
                          "id": "root",
                          "topic": "JabMap",
                          "expanded": true,
                          "icons": [],
                          "highlight": null,
                          "type": "Text"
                        }
                      }
                    }
                    """;
        }
        return Files.readString(jabMapPath);
    }

    /**
     * At http://localhost:23119/libraries/{id}/map <br><br>
     * <p>
     * Saves the mindmap next to its associated library.
     *
     * @param id The given library
     * @throws IOException
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateJabMapJson(@PathParam("id") String id, String fileContent) throws IOException {
        boolean isDemo = "demo".equals(id);
        java.nio.file.Path targetPath;
        if (isDemo) {
            targetPath = getJabMapDemoPath();
        } else {
            targetPath = getJabMapPath(id);
        }
        Files.writeString(targetPath, fileContent);
    }

    private java.nio.file.Path getJabMapPath(String id) {
        java.nio.file.Path libraryPath = ServerUtils.getLibraryPath(id, filesToServe, srvStateManager);
        String newName = libraryPath.getFileName().toString().replaceFirst("\\.bib$", ".jmp");
        return libraryPath.getParent().resolve(newName);
    }

    private java.nio.file.Path getJabMapDemoPath() {
        java.nio.file.Path result = java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).resolve("demo.jmp");
        LOGGER.debug("Using temporary file for demo jmp: {}", result);
        return result;
    }
}
