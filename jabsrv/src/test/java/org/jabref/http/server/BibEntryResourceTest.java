package org.jabref.http.server;

import org.jabref.http.JabrefMediaType;
import org.jabref.http.server.resources.BibEntryResource;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(BibEntryResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void getJson() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request().get(String.class);
        // Basic sanity check: response is a JSON array and contains the entry id
        assertTrue(response.trim().startsWith("["));
        assertTrue(response.contains("\"Author2023test\""));
    }

    @Test
    void getCslItemJson() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.JSON_CSL_ITEM).get(String.class);
        assertEquals("""
                [{"id":"Author2023test","type":"article","author":[{"family":"Author","given":"Demo"}],"event-date":{"date-parts":[[2023]]},"issued":{"date-parts":[[2023]]},"title":"Demo Title"}]""", response);
    }

    @Test
    void getBibtex() {
        // For non-demo libraries the test reads the test file content; this asserts we get a BibTeX-like response
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.BIBTEX).get(String.class);
        assertTrue(response.contains("@Misc{Author2023test"));
        assertTrue(response.contains("jabref-meta: databaseType:bibtex"));
    }
}