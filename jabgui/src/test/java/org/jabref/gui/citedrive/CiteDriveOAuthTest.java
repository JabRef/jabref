package org.jabref.gui.citedrive;

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
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
    void getToken() throws ExecutionException, InterruptedException {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));

        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class));
        // When testing with https://github.com/navikt/mock-oauth2-server
        // CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, OAUTH_SESSION_REGISTRY, mock(DialogService.class), URI.create("http://localhost:8080/default/authorize"), URI.create("http://localhost:8080/default/token"));

        Optional<Tokens> actual = citeDriveOAuthService.authorizeInteractive().get();
        assertTrue(actual.isPresent());
    }
}
