package org.jabref.http.server;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.jabref.http.JabrefMediaType;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryResourceTest extends ServerTest{

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibraryResource.class, LibrariesResource.class,BibEntryResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void getCSLJsonRepresentation() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(JabrefMediaType.JSON_CSL_ITEM)
                .get(String.class);

        // 1. Vérifie que la réponse est bien un tableau JSON
        assertTrue(response.startsWith("[") && response.endsWith("]"),
                "CSL JSON doit être un tableau JSON");

        // 2. Vérifie que l’objet contient au moins l’ID attendu
        assertTrue(response.contains("\"id\":\"Author2023test\""),
                "La réponse doit contenir l'identifiant CSL de l'entrée");

        // 3. Vérifie qu’on a bien un champ CSL standard (ex: title)
        assertTrue(response.contains("\"title\""),
                "CSL JSON doit contenir un champ 'title'");
    }
    @Test
    void getJsonRepresentation() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

        // 1. Vérifie que la réponse est bien un tableau JSON
        assertTrue(response.startsWith("[") && response.endsWith("]"),
                "La réponse JSON doit être un tableau");

        // 2. Vérifie que la clé de citation attendue est bien présente
        assertTrue(response.contains("\"citationKey\":\"Author2023test\""),
                "La réponse doit contenir la clé de citation 'Author2023test'");
    }

}
