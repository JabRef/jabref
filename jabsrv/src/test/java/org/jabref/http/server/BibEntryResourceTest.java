package org.jabref.http.server;

import org.jabref.http.JabrefMediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        String expected = """
                [
                  {
                    "id": "Author2023test",
                    "type": "ARTICLE",
                    "author": [
                      {
                        "family": "Author",
                        "given": "Demo"
                      }
                    ],
                    "eventDate": {
                      "dateParts": [
                        [
                          2023
                        ]
                      ]
                    },
                    "issued": {
                      "dateParts": [
                        [
                          2023
                        ]
                      ]
                    },
                    "title": "Demo Title"
                  }
                ]
                """;

        JsonNode root = mapper.readTree(response);
        JsonNode expectedJson = mapper.readTree(expected);

        assertEquals(root, expectedJson, "CSL JSON must match the expected structure");
    }

    @Test
    void getJsonRepresentation() throws Exception {
        String response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/entries/Author2023test")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
        String expected = """
                [
                  {
                    "sharingMetadata": {
                      "sharedID": -1,
                      "version": 1
                    },
                    "userComments": "",
                    "citationKey": "Author2023test",
                    "bibtex": "@Misc{Author2023test,\\n  author \\u003d {Demo Author},\\n  title  \\u003d {Demo Title},\\n  year   \\u003d {2023},\\n}\\n"
                  }
                ]
                """;

        JsonNode root = mapper.readTree(response);

        JsonNode expectedJson = mapper.readTree(expected);

        assertEquals(root, expectedJson, "JSON must match the expected structure");
    }
}
