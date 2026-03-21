package org.jabref.http.server;

import org.jabref.http.server.resources.RootResource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CORSFilterTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class);
        resourceConfig.register(CORSFilter.class);
        addPreferencesToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void allowedOriginReturnsSpecificACAO() {
        Response response = target("/")
                .request()
                .header("Origin", "chrome-extension://test123")
                .get();
        assertEquals("chrome-extension://test123", response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void firefoxExtensionOriginReturnsSpecificACAO() {
        Response response = target("/")
                .request()
                .header("Origin", "moz-extension://random-uuid")
                .get();
        assertEquals("moz-extension://random-uuid", response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void jabrefGithubOriginReturnsSpecificACAO() {
        Response response = target("/")
                .request()
                .header("Origin", "https://jabref.github.io")
                .get();
        assertEquals("https://jabref.github.io", response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void disallowedOriginReturnsNoACAO() {
        Response response = target("/")
                .request()
                .header("Origin", "https://evil.example.com")
                .get();
        assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void responseContainsExtendedAllowHeaders() {
        Response response = target("/")
                .request()
                .header("Origin", "chrome-extension://test123")
                .get();
        String allowHeaders = response.getHeaderString("Access-Control-Allow-Headers");
        assertNotNull(allowHeaders);
        assertTrue(allowHeaders.contains("X-JabRef-Connector"));
        assertTrue(allowHeaders.contains("Authorization"));
    }

    @Test
    void responseContainsVaryOriginHeader() {
        Response response = target("/")
                .request()
                .header("Origin", "chrome-extension://test123")
                .get();
        String vary = response.getHeaderString("Vary");
        assertNotNull(vary);
        assertTrue(vary.contains("Origin"));
    }

    @Test
    void responseContainsMaxAgeHeader() {
        Response response = target("/")
                .request()
                .header("Origin", "chrome-extension://test123")
                .get();
        assertEquals("600", response.getHeaderString("Access-Control-Max-Age"));
    }

    @Test
    void requestWithoutOriginReturnsNoACAO() {
        Response response = target("/")
                .request()
                .get();
        assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
    }
}
