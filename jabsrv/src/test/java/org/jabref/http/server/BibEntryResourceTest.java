package org.jabref.http.server;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void getPlainRepresentation() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(MediaType.TEXT_PLAIN)
                .get(String.class);

        assertEquals("""
                Author: Demo Author
                Title: Demo Title
                Journal: (N/A)
                Volume: (N/A)
                Number: (N/A)
                Pages: (N/A)
                Released on: (N/A)""", response);
    }

    @Test
    void getHTMLRepresentation() {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(MediaType.TEXT_HTML)
                .get(String.class);

        assertEquals(
                "<strong>Author:</strong> Demo Author<br>" +
                        "<strong>Title:</strong> Demo Title<br>" +
                        "<strong>Journal:</strong> (N/A)<br>" +
                        "<strong>Volume:</strong> (N/A)<br>" +
                        "<strong>Number:</strong> (N/A)<br>" +
                        "<strong>Pages:</strong> (N/A)<br>" +
                        "<strong>Released on:</strong> (N/A)", response);
    }

}
