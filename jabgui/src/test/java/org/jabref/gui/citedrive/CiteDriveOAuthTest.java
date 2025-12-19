package org.jabref.gui.citedrive;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.http.SrvStateManager;
import org.jabref.http.server.manager.HttpServerManager;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CiteDriveOAuthTest {

    private static final HttpServerManager HTTP_SERVER_MANAGER = new HttpServerManager();
    private static final OAuthSessionRegistry OAUTH_SESSION_REGISTRY = new OAuthSessionRegistry();

    @BeforeAll
    static void startServer() {
        CliPreferences cliPreferences = mock(CliPreferences.class);
        SrvStateManager srvStateManager = mock(SrvStateManager.class);
        URI uri = URI.create("http://localhost:23119");
        HTTP_SERVER_MANAGER.start(cliPreferences, srvStateManager, OAUTH_SESSION_REGISTRY, uri);
    }

    @AfterAll
    static void stopServer() {
        HTTP_SERVER_MANAGER.stop();
    }

    @Test
    @Timeout(60)
    @Disabled
    void getToken() throws ExecutionException, InterruptedException {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));

        CiteDrivePreferences citeDrivePreferences = mock(CiteDrivePreferences.class);

        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, citeDrivePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class));
        // When testing with https://github.com/navikt/mock-oauth2-server
        // CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class), URI.create("http://localhost:8080/default/authorize"), URI.create("http://localhost:8080/default/token"));

        Optional<AccessToken> actual = citeDriveOAuthService.authorizeInteractive().get();
        assertTrue(actual.isPresent());
    }

    /// Fetches a token and sends some BibTeX to CiteDrive
    @Test
    @Timeout(60)
    @Disabled
    void putLibraryOAuthFromScratch() throws ExecutionException, InterruptedException, IOException {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));

        CiteDrivePreferences citeDrivePreferences = mock(CiteDrivePreferences.class);

        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, citeDrivePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class));

        Optional<AccessToken> actual = citeDriveOAuthService.authorizeInteractive().get();
        assertTrue(actual.isPresent());

        String bibtex = """
                @article{xyz,
                  author = {Doe, John and Smith, Jane},
                  title = {An Example Article},
                  journal = {Journal of Examples},
                  year = {2024},
                  volume = {42},
                  number = {1},
                  pages = {1-10},
                  publisher = {Example Publisher}
                }
                """;

        HttpResponse<String> response = Unirest.post("https://api-dev.citedrive.com/jabref/push/")
                                               .header("Authorization", "Bearer " + actual.get())
                                               .header("Content-Type", MediaTypes.APPLICATION_BIBTEX)
                                               .body(bibtex)
                                               .asString();

        assertEquals(200, response.getStatus());
        // Example response: {"file_size":225,"file_size_kb":0.22,"entry_count":1,"status":"success"}
        assertNotEquals("", response.getBody());
    }

    /// Uses the "intended" use of CiteDriveOAuthService w/ token refresh
    @Test
    @Timeout(60)
    @Disabled
    void putLibrary() throws ExecutionException, InterruptedException, IOException {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));

        CiteDrivePreferences citeDrivePreferences = mock(CiteDrivePreferences.class);

        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, citeDrivePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class));

        Optional<AccessToken> actual = citeDriveOAuthService.currentOrFreshTokens().get();
        assertTrue(actual.isPresent());

        String bibtex = """
                @article{xyz,
                  author = {Doe, John and Smith, Jane},
                  title = {An Example Article},
                  journal = {Journal of Examples},
                  year = {2024},
                  volume = {42},
                  number = {1},
                  pages = {1-10},
                  publisher = {Example Publisher}
                }
                """;

        HttpResponse<String> response = Unirest.post("https://api-dev.citedrive.com/jabref/push/")
                                               .header("Authorization", "Bearer " + actual.get())
                                               .header("Content-Type", MediaTypes.APPLICATION_BIBTEX)
                                               .body(bibtex)
                                               .asString();

        assertEquals(200, response.getStatus());
        // Example response: {"file_size":225,"file_size_kb":0.22,"entry_count":1,"status":"success"}
        assertNotEquals("", response.getBody());
    }
}
