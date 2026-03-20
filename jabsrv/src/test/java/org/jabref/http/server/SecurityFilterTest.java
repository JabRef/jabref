package org.jabref.http.server;

import java.util.List;

import org.jabref.http.server.resources.LibrariesResource;
import org.jabref.http.server.resources.PairingResource;
import org.jabref.http.server.resources.RootResource;
import org.jabref.logic.remote.server.ConnectorAuthenticationTask;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityFilterTest extends ServerTest {

    private static ConnectorAuthenticationTask connectorAuthentication;

    @BeforeAll
    static void initConnectorAuthentication() {
        connectorAuthentication = new ConnectorAuthenticationTask(serverTestRemotePreferences);
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class, LibrariesResource.class, PairingResource.class);
        resourceConfig.register(SecurityFilter.class);
        resourceConfig.register(CORSFilter.class);
        addPreferencesToResourceConfig(resourceConfig);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addConnectorAuthenticationTaskToResourceConfig(resourceConfig, connectorAuthentication);
        return resourceConfig.getApplication();
    }

    @AfterEach
    void resetAllowUnauthenticatedWithoutOrigin() {
        serverTestRemotePreferences.setAllowUnauthenticatedAccessWithoutOrigin(false);
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
    void requestWithoutOriginRequiresBearerWhenStrict() {
        Response response = target("/libraries")
                .request()
                .get();
        assertEquals(401, response.getStatus());
    }

    @Test
    void requestWithoutOriginWithValidBearerIsAllowedWhenStrict() {
        String pin = connectorAuthentication.generatePin();
        String token = connectorAuthentication.validatePinAndGenerateToken(pin).orElseThrow();

        Response response = target("/libraries")
                .request()
                .header("Authorization", "Bearer " + token)
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void requestWithoutOriginAllowedWhenPreferenceRelaxesAuth() {
        serverTestRemotePreferences.setAllowUnauthenticatedAccessWithoutOrigin(true);
        Response response = target("/libraries")
                .request()
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    void pairingWithoutOriginDoesNotRequireBearerWhenStrict() {
        Response response = target("/auth/pair")
                .request()
                .post(Entity.json("{\"pin\":\"000000\"}"));
        assertEquals(403, response.getStatus());
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
        String pin = connectorAuthentication.generatePin();
        String token = connectorAuthentication.validatePinAndGenerateToken(pin).orElseThrow();

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
