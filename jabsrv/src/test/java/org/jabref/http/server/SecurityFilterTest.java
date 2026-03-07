package org.jabref.http.server;

import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.RootResource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityFilterTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class, LibrariesResource.class);
        resourceConfig.register(SecurityFilter.class);
        resourceConfig.register(CORSFilter.class);
        addPreferencesToResourceConfig(resourceConfig);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void optionsRequestIsAllowed() {
        Response response = target("/libraries")
                .request()
                .options();
        assertEquals(200, response.getStatus());
    }

    @Test
    void healthCheckWithoutHeadersIsAllowed() {
        Response response = target("/")
                .request()
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void requestWithValidOriginAndCustomHeaderIsAllowed() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "chrome-extension://test123")
                .header("X-JabRef-Connector", "")
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void requestWithValidOriginButMissingCustomHeaderIsRejected() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "chrome-extension://test123")
                .get();
        assertEquals(403, response.getStatus());
    }

    @Test
    void requestWithInvalidOriginIsRejected() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "https://evil.example.com")
                .header("X-JabRef-Connector", "")
                .get();
        assertEquals(403, response.getStatus());
    }

    @Test
    void requestWithoutOriginIsAllowed() {
        Response response = target("/libraries")
                .request()
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void firefoxExtensionOriginWithCustomHeaderIsAllowed() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "moz-extension://random-uuid")
                .header("X-JabRef-Connector", "")
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void exactMatchOriginIsAllowedWithoutCustomHeader() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "https://jabref.github.io")
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void prefixMatchOriginWithoutCustomHeaderIsRejected() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "moz-extension://random-uuid")
                .get();
        assertEquals(403, response.getStatus());
    }
}
