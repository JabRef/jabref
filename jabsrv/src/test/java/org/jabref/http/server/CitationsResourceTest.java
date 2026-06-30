package org.jabref.http.server;

import org.jabref.http.server.resources.CitationsResource;
import org.jabref.http.server.services.CitationCacheService;
import org.jabref.logic.UiMessageHandler;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationsResourceTest extends ServerTest {

    private UiMessageHandler uiMessageHandler;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        // CALLS_REAL_METHODS so the default isGuiConnected() runs (this != NONE -> true);
        // a plain mock would return false and every request would be rejected with 400.
        uiMessageHandler = Mockito.mock(UiMessageHandler.class, Mockito.CALLS_REAL_METHODS);
        super.setUp();
    }

    @Override
    protected Application configure() {
        // addGuiBridgeToResourceConfig opens the GENERAL_SERVER_TEST library, so its id resolves.
        ResourceConfig resourceConfig = new ResourceConfig(CitationsResource.class);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        addEntryTypesManagerToResourceConfig(resourceConfig);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(uiMessageHandler).to(UiMessageHandler.class);
                bind(new CitationCacheService()).to(CitationCacheService.class);
            }
        });
        return resourceConfig.getApplication();
    }

    private Response postAddFromCache(String id, String cacheKey) {
        return target("/libraries/" + id + "/citations/" + cacheKey)
                .request()
                .post(Entity.text(""));
    }

    @Test
    void addToDemoLibraryReturns404() {
        // The path-less "demo" library is not an open library; resolving it as an append target must
        // 404 rather than silently appending to the active library. The 404 is raised before the
        // cache lookup, so an arbitrary cache key is fine here.
        Response response = postAddFromCache("demo", "any-cache-key");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void addToUnknownLibraryReturns404() {
        Response response = postAddFromCache("does-not-exist", "any-cache-key");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void addToOpenLibraryWithExpiredKeyReturns410() {
        // An open library id resolves to a real target, so the request proceeds to the cache lookup,
        // which misses (no key was issued) and reports 410 Gone.
        Response response = postAddFromCache(TestBibFile.GENERAL_SERVER_TEST.id, "expired-or-unknown");

        assertEquals(Response.Status.GONE.getStatusCode(), response.getStatus());
    }

    @Test
    void lookupBlankTextReturns400() {
        Response response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/citations/lookup")
                .request()
                .post(Entity.text("   "));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void lookupOnUnknownLibraryReturns404() {
        Response response = target("/libraries/does-not-exist/citations/lookup")
                .request()
                .post(Entity.text("Smith, J. (2020). Chocolate. J. Sweets, 12, 45."));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void batchLookupEmptyListReturns400() {
        Response response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/citations/lookup:batch")
                .request()
                .post(Entity.json("{\"citations\":[]}"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void batchLookupBlankSlotReturnsNoneScope() {
        // All-blank batch never invokes the parser; each slot short-circuits to a "none" response,
        // exercising the batch wiring + JSON serialization without needing a real plain-citation parser.
        Response response = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/citations/lookup:batch")
                .request()
                .post(Entity.json("{\"citations\":[\"\",\"   \"]}"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = response.readEntity(String.class);
        assertTrue(body.contains("\"matchScope\":\"none\""), body);
    }
}
