package org.jabref.http.server;

import java.util.List;

import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.PairingResource;
import org.jabref.http.server.resources.RootResource;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.server.ConnectorTokenManager;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityFilterTest extends ServerTest {

    private static final RemotePreferences TEST_REMOTE_PREFS = new RemotePreferences(
            6050, true, 23119, false, false, 2087,
            List.of("chrome-extension://", "moz-extension://", "https://jabref.github.io", "https://jabref.org"),
            "");

    private static final ConnectorTokenManager TOKEN_MANAGER = new ConnectorTokenManager(TEST_REMOTE_PREFS);

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class, LibrariesResource.class, PairingResource.class);
        resourceConfig.register(SecurityFilter.class);
        resourceConfig.register(CORSFilter.class);
        addPreferencesToResourceConfig(resourceConfig);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addTokenManagerToResourceConfig(resourceConfig, TOKEN_MANAGER);
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
    void requestWithoutOriginIsAllowed() {
        Response response = target("/libraries")
                .request()
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void exactMatchOriginIsAllowedWithoutToken() {
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

    @Test
    void prefixMatchOriginWithCustomHeaderButNoTokenIsUnauthorized() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "chrome-extension://test123")
                .header("X-JabRef-Connector", "true")
                .get();
        assertEquals(401, response.getStatus());
    }

    @Test
    void prefixMatchOriginWithValidTokenIsAllowed() {
        String pin = TOKEN_MANAGER.generatePin();
        String token = TOKEN_MANAGER.validatePinAndGenerateToken(pin).orElseThrow();

        Response response = target("/libraries")
                .request()
                .header("Origin", "chrome-extension://test123")
                .header("X-JabRef-Connector", "true")
                .header("Authorization", "Bearer " + token)
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void prefixMatchOriginWithInvalidTokenIsUnauthorized() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "chrome-extension://test123")
                .header("X-JabRef-Connector", "true")
                .header("Authorization", "Bearer invalid-token")
                .get();
        assertEquals(401, response.getStatus());
    }

    @Test
    void requestWithInvalidOriginIsRejected() {
        Response response = target("/libraries")
                .request()
                .header("Origin", "https://evil.example.com")
                .header("X-JabRef-Connector", "true")
                .header("Authorization", "Bearer something")
                .get();
        assertEquals(403, response.getStatus());
    }

    @Test
    void pairingEndpointAllowedWithoutToken() {
        Response response = target("/auth/pair")
                .request()
                .header("Origin", "chrome-extension://test123")
                .header("X-JabRef-Connector", "true")
                .post(Entity.json("{\"pin\":\"000000\"}"));
        // 403 from PairingResource (invalid PIN), NOT 401 (token not required for pairing)
        assertEquals(403, response.getStatus());
    }
}
