package org.jabref.http.server;

import org.jabref.http.JabrefMediaType;
import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.LibraryResource;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibraryResource.class, LibrariesResource.class);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addEntryTypesManagerToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void getJson() {
        assertEquals("""
                        @Misc{Author2023test,
                          author = {Demo Author},
                          title  = {Demo Title},
                          year   = {2023},
                        }

                        @Article{doi2023entry,
                          author = {Test Author},
                          doi    = {10.1000/xyz123},
                          title  = {Test DOI Entry},
                          year   = {2023},
                        }

                        @Misc{url2023entry,
                          author = {Test Author},
                          title  = {Test URL Entry},
                          url    = {https://example.com/paper},
                          year   = {2023},
                        }

                        @Comment{jabref-meta: databaseType:bibtex;}
                        """,
                target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.BIBTEX).get(String.class));
    }

    @Test
    void getClsItemJson() {
        assertEquals("""
                        [{"id":"Author2023test","type":"article","author":[{"family":"Author","given":"Demo"}],"event-date":{"date-parts":[[2023]]},"issued":{"date-parts":[[2023]]},"title":"Demo Title"},{"id":"doi2023entry","type":"article-journal","author":[{"family":"Author","given":"Test"}],"event-date":{"date-parts":[[2023]]},"issued":{"date-parts":[[2023]]},"DOI":"10.1000/xyz123","title":"Test DOI Entry"},{"id":"url2023entry","type":"article","author":[{"family":"Author","given":"Test"}],"event-date":{"date-parts":[[2023]]},"issued":{"date-parts":[[2023]]},"title":"Test URL Entry","URL":"https://example.com/paper"}]""",
                target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.JSON_CSL_ITEM).get(String.class));
    }
}
