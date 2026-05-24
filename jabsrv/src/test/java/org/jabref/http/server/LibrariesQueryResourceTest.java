package org.jabref.http.server;

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
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void singleQueryReturnsMatches() {
        // [utest->req~jabsrv.query.search~1]
        String body = """
                {"queries": ["author = \\"Test Author\\""]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "results": [
                    {
                      "query": "author \\u003d \\"Test Author\\"",
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
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id, TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }

    @Test
    void queriesAreReturnedInInputOrder() {
        // [utest->req~jabsrv.query.search~1]
        String body = """
                {"queries": ["title = \\"Test DOI Entry\\"", "title = \\"Demo Title\\""]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "results": [
                    {
                      "query": "title \\u003d \\"Test DOI Entry\\"",
                      "matches": [
                        {
                          "libraryId": "%s",
                          "entryId": "doi2023entry"
                        }
                      ]
                    },
                    {
                      "query": "title \\u003d \\"Demo Title\\"",
                      "matches": [
                        {
                          "libraryId": "%s",
                          "entryId": "Author2023test"
                        }
                      ]
                    }
                  ]
                }""".formatted(TestBibFile.GENERAL_SERVER_TEST.id, TestBibFile.GENERAL_SERVER_TEST.id);
        assertEquals(expected, result);
    }

    @Test
    void queryWithoutMatchReturnsEmptyMatches() {
        // [utest->req~jabsrv.query.search~1]
        String body = """
                {"queries": ["author = \\"Nobody\\""]}""";
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        String expected = """
                {
                  "results": [
                    {
                      "query": "author \\u003d \\"Nobody\\"",
                      "matches": []
                    }
                  ]
                }""";
        assertEquals(expected, result);
    }

    @Test
    void emptyRequestReturnsEmptyResults() {
        String result = target("/libraries/query")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json("{}"), String.class);
        assertEquals("""
                {
                  "results": []
                }""", result);
    }
}
