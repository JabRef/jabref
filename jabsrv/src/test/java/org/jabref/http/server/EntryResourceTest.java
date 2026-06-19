package org.jabref.http.server;

import java.util.EnumSet;

import org.jabref.http.server.resources.EntryResource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(EntryResource.class);
        addGuiBridgeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void htmlRepresentationEscapesFieldValues() {
        setAvailableLibraries(EnumSet.of(TestBibFile.XSS_SERVER_TEST));

        String response = target("/libraries/" + TestBibFile.XSS_SERVER_TEST.id + "/entries/Xss2026/entries/Xss2026")
                .request()
                .get(String.class);

        assertFalse(response.contains("<script>alert('xss')</script>"));
        assertFalse(response.contains("<img src=x onerror=alert(1)>"));
        assertTrue(response.contains("Alice &lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; &amp; Bob"));
        assertTrue(response.contains("Title &quot;quoted&quot; &lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(response.contains("Journal &gt; Proceedings"));
    }

    @Test
    void htmlRepresentationEscapesCitationKeyInNotFoundMessage() {
        setAvailableLibraries(EnumSet.of(TestBibFile.XSS_SERVER_TEST));

        String maliciousKey = "<script>alert('xss')</script>";
        Response response = target("/libraries/{id}/entries/{key1}/entries/{key2}")
                .resolveTemplate("id", TestBibFile.XSS_SERVER_TEST.id)
                .resolveTemplate("key1", maliciousKey)
                .resolveTemplate("key2", maliciousKey)
                .request()
                .get();

        assertEquals(404, response.getStatus());

        String body = response.readEntity(String.class);
        assertFalse(body.contains("<script>alert('xss')</script>"));
        assertTrue(body.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
    }
}
