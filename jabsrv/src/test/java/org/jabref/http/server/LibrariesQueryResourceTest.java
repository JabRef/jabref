package org.jabref.http.server;

import java.util.EnumSet;

import org.jabref.http.server.resources.LibrariesResource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibrariesQueryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibrariesResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void doiMatchReturnsEntry() {
        // [utest->req~jabsrv.query.doi~1]
        String body = """
                {"dois": ["10.1000/xyz123"]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "matches": [
                    {
                      "doi": "10.1000/xyz123",
                      "libraryId": "%s",
                      "entryId": "doi2023entry"
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }

    @Test
    void doiNoMatchReturnsEmptyMatches() {
        // [utest->req~jabsrv.query.doi~1]
        String body = """
                {"dois": ["10.9999/notfound"]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        assertEquals("""
                {
                  "matches": []
                }""", result);
    }

    @Test
    void urlMatchReturnsEntry() {
        // [utest->req~jabsrv.query.url~1]
        String body = """
                {"urls": ["https://example.com/paper"]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "matches": [
                    {
                      "url": "https://example.com/paper",
                      "libraryId": "%s",
                      "entryId": "url2023entry"
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }

    @Test
    void doiMatchInChocolateBib() {
        // [utest->req~jabsrv.query.doi~1]
        setAvailableLibraries(EnumSet.of(TestBibFile.CHOCOLATE_BIB));
        String body = """
                {"dois": ["10.1161/circulationaha.108.827022"]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "matches": [
                    {
                      "doi": "10.1161/circulationaha.108.827022",
                      "libraryId": "%s",
                      "entryId": "Corti_2009"
                    }
                  ]
                }""".formatted(TestBibFile.CHOCOLATE_BIB.id);
        assertEquals(expected, result);
    }

    @Test
    void emptyRequestReturnsEmptyMatches() {
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json("{}"), String.class);
        assertEquals("""
                {
                  "matches": []
                }""", result);
    }

    @Test
    void searchQueryReturnsEntryWithoutDoiOrUrl() {
        String body = """
                {"query": "author = \\"Test Author\\""}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "matches": [
                    {
                      "libraryId": "%s",
                      "entryId": "doi2023entry"
                    },
                    {
                      "libraryId": "%s",
                      "entryId": "url2023entry"
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id, TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }

    @Test
    void doiNormalisationStripsHttpsPrefix() {
        // [utest->req~jabsrv.query.doi~1]
        String body = """
                {"dois": ["https://doi.org/10.1000/xyz123"]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "matches": [
                    {
                      "doi": "10.1000/xyz123",
                      "libraryId": "%s",
                      "entryId": "doi2023entry"
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }
}
