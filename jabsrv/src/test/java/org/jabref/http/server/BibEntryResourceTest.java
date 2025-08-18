package org.jabref.http.server;

import org.jabref.http.JabrefMediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryResourceTest extends ServerTest {


    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(
                LibraryResource.class,
                LibrariesResource.class,
                BibEntryResource.class
        );
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void getCSLJsonRepresentation() throws Exception {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(JabrefMediaType.JSON_CSL_ITEM)
                .get(String.class);

        JsonNode root = mapper.readTree(response);

        assertTrue(root.isArray(), "CSL JSON must be a JSON array");

        assertEquals("Author2023test", root.get(0).get("id").asText(), "Response must contain the CSL entry ID");

        assertTrue(root.get(0).has("title"), "CSL JSON must contain a 'title' field");
    }

    @Test
    void getJsonRepresentation() throws Exception {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

        JsonNode root = mapper.readTree(response);

        assertTrue(root.isArray(), "JSON response must be an array");

        assertEquals("Author2023test", root.get(0).get("citationKey").asText(),
                "Response must contain the citation key 'Author2023test'");
    }
}
