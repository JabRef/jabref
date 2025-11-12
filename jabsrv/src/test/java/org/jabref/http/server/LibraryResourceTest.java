package org.jabref.http.server;

import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.LibraryResource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for LibraryResource which provides the /libraries/{id}/entries/pdffiles endpoint
 */
class LibraryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibraryResource.class, LibrariesResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void getPDFFilesAsListReturnsEmptyArrayWhenNoPDFs() {
        // The test library has no PDF files attached, so we expect an empty JSON array
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/pdffiles")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

        // The API returns an empty JSON array when there are entries but no linked PDF files
        assertEquals("[]", response.trim());
    }

    @Test
    void getPDFFilesAsListWithDemoLibrary() {
        // The demo library (Chocolate.bib) might have PDF entries
        // This tests the endpoint exists and returns valid JSON
        String response = target("/libraries/demo/entries/pdffiles")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

        // Response should be a valid JSON array (either empty [] or with entries)
        // At minimum, verify it starts with [ to indicate it's a JSON array
        assertEquals('[', response.charAt(0), "Response should be a JSON array");
    }
}
