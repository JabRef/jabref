package org.jabref.http.server.cayw;

import javafx.collections.FXCollections;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.ServerTest;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CAYWResourceTest extends ServerTest {
    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(CAYWResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addFormatterServiceToResourceConfig(resourceConfig);

        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                SrvStateManager mockSrv = Mockito.mock(SrvStateManager.class);
                BibEntry bibEntry = new BibEntry().withCitationKey("Author2023test");
                Mockito.when(mockSrv.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(bibEntry));
                bind(mockSrv).to(SrvStateManager.class);
            }
        });

        return resourceConfig.getApplication();
    }

    @Test
    void probe_returns_ready() {
        Response response = target("/better-bibtex/cayw")
                .queryParam("probe", "1")
                .request()
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("ready", response.readEntity(String.class));
        assertEquals("text/plain", response.getHeaderString("Content-Type"));
    }

    @Test
    void biblatex() {
        Response response = target("/better-bibtex/cayw")
                .queryParam("format", "biblatex")
                .queryParam("selected", "1")
                .request()
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("\\autocite{Author2023test}", response.readEntity(String.class));
        assertEquals("text/plain", response.getHeaderString("Content-Type"));
    }
}
