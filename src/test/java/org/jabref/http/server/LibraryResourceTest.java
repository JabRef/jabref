package org.jabref.http.server;

import org.jabref.http.JabrefMediaType;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibraryResource.class, LibrariesResource.class);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
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

                @Comment{jabref-meta: databaseType:bibtex;}
                """, target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.BIBTEX).get(String.class));
    }

    @Test
    void getClsItemJson() {
        assertEquals("""
                [{"id":"Author2023test","type":"article","author":[{"family":"Author","given":"Demo"}],"event-date":{"date-parts":[[2023]]},"issued":{"date-parts":[[2023]]},"title":"Demo Title"}]""", target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id).request(JabrefMediaType.JSON_CSL_ITEM).get(String.class));
    }
}
