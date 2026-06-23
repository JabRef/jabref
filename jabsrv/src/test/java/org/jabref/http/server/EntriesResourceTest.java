package org.jabref.http.server;

import java.util.List;

import org.jabref.http.server.resources.EntriesResource;
import org.jabref.logic.UiCommand;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.importer.util.MediaTypes;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntriesResourceTest extends ServerTest {

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
        ResourceConfig resourceConfig = new ResourceConfig(EntriesResource.class);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(uiMessageHandler).to(UiMessageHandler.class);
            }
        });
        return resourceConfig.getApplication();
    }

    private Response postBibtex(String id, String bibtex) {
        return target("/libraries/" + id + "/entries")
                .request()
                .post(Entity.entity(bibtex, MediaTypes.APPLICATION_BIBTEX));
    }

    @SuppressWarnings("unchecked")
    private UiCommand captureSingleCommand() {
        ArgumentCaptor<List<UiCommand>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(uiMessageHandler).handleUiCommands(captor.capture());
        List<UiCommand> commands = captor.getValue();
        assertEquals(1, commands.size());
        return commands.getFirst();
    }

    @Test
    void importToOpenLibrarySwitchesToThatLibrary() {
        Response response = postBibtex(TestBibFile.GENERAL_SERVER_TEST.id, "@article{a, title={t}}");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        UiCommand.AppendBibTeXToLibrary command = (UiCommand.AppendBibTeXToLibrary) captureSingleCommand();
        assertEquals(java.util.Optional.of(TestBibFile.GENERAL_SERVER_TEST.path), command.library());
    }

    @Test
    void importToCurrentLibraryHasEmptyTarget() {
        Response response = postBibtex("current", "@article{a, title={t}}");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        UiCommand.AppendBibTeXToLibrary command = (UiCommand.AppendBibTeXToLibrary) captureSingleCommand();
        assertTrue(command.library().isEmpty());
    }

    @Test
    void importToUnknownLibraryReturns404() {
        Response response = postBibtex("does-not-exist", "@article{a, title={t}}");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        Mockito.verifyNoInteractions(uiMessageHandler);
    }

    @Test
    void importEmptyBibtexReturns400() {
        Response response = postBibtex(TestBibFile.GENERAL_SERVER_TEST.id, "   ");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        Mockito.verifyNoInteractions(uiMessageHandler);
    }
}
