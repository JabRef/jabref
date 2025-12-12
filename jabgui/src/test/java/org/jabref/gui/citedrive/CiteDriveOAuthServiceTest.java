package org.jabref.gui.citedrive;

import java.net.URI;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.http.SrvStateManager;
import org.jabref.http.server.manager.HttpServerManager;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CiteDriveOAuthServiceTest {

    private static final HttpServerManager HTTP_SERVER_MANAGER = new HttpServerManager();

    @BeforeAll
    static void startServer() {
        CliPreferences cliPreferences = mock(CliPreferences.class);
        SrvStateManager srvStateManager = mock(SrvStateManager.class);
        URI uri = URI.create("http://localhost:23119");
        HTTP_SERVER_MANAGER.start(cliPreferences, srvStateManager, uri);
    }

    @AfterAll
    static void stopServer() {
        HTTP_SERVER_MANAGER.stop();
    }

    @Test
    @Disabled
    void getToken() throws ExecutionException, InterruptedException {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));

        OAuthSessionRegistry oAuthSessionRegistry = new OAuthSessionRegistry();
        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, oAuthSessionRegistry);

        Optional<Tokens> actual = citeDriveOAuthService.authorizeInteractive().get();
        assertTrue(actual.isPresent());
    }
}
