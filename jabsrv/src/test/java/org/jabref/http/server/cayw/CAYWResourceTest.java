package org.jabref.http.server.cayw;

import javafx.collections.FXCollections;

import org.jabref.http.SrvStateManager;
import org.jabref.http.server.ServerTest;
import org.jabref.http.server.TestBibFile;
import org.jabref.http.server.services.FilesToServe;
import org.jabref.model.entry.BibEntry;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CAYWResourceTest extends ServerTest {
    private final FilesToServe filesToServe = new FilesToServe();

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        filesToServe.setFilesToServe(FXCollections.observableArrayList(TestBibFile.GENERAL_SERVER_TEST.path));
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(CAYWResource.class);
        addPreferencesToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addFormatterServiceToResourceConfig(resourceConfig);

        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(filesToServe).to(FilesToServe.class);
                SrvStateManager mockSrv = Mockito.mock(SrvStateManager.class);
                BibEntry bibEntry = new BibEntry().withCitationKey("Author2023test");
                Mockito.when(mockSrv.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(bibEntry));
                Mockito.when(mockSrv.getOpenDatabases()).thenReturn(FXCollections.observableArrayList());
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
        assertEquals("nosniff", response.getHeaderString("X-Content-Type-Options"));
        assertEquals("default-src 'none'; frame-ancestors 'none'; base-uri 'none'", response.getHeaderString("Content-Security-Policy"));
    }

    @Test
    void maliciousCommandReturnsBadRequest() {
        Response response = target("/better-bibtex/cayw")
                .queryParam("format", "biblatex")
                .queryParam("selected", "1")
                .queryParam("application", "Sublime Text")
                .queryParam("command", "'; touch /tmp/pwned; #")
                .request()
                .get();

        assertEquals(400, response.getStatus());
        assertEquals("The 'command' parameter contains invalid characters. Only letters (A–Z, a–z) and '*' are allowed.", response.readEntity(String.class));
    }

    @Test
    void servedLibraryPathIsAccepted() {
        Response response = target("/better-bibtex/cayw")
                .queryParam("format", "biblatex")
                .queryParam("selected", "1")
                .queryParam("librarypath", TestBibFile.GENERAL_SERVER_TEST.path.toString())
                .request()
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("\\autocite{Author2023test}", response.readEntity(String.class));
    }

    @Test
    void unknownLibraryPathReturnsBadRequest() {
        Response response = target("/better-bibtex/cayw")
                .queryParam("format", "biblatex")
                .queryParam("selected", "1")
                .queryParam("librarypath", "/tmp/not-served-library.bib")
                .request()
                .get();

        assertEquals(400, response.getStatus());
    }

    @Test
    void nullFilesToServeListDoesNotCrash() {
        filesToServe.setFilesToServe(null);

        Response response = target("/better-bibtex/cayw")
                .queryParam("format", "biblatex")
                .queryParam("selected", "1")
                .queryParam("librarypath", "/tmp/not-served-library.bib")
                .request()
                .get();

        assertEquals(400, response.getStatus());
    }
}
