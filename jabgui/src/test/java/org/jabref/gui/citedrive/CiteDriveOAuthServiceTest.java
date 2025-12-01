package org.jabref.gui.citedrive;

import java.net.URI;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.remote.RemotePreferences;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CiteDriveOAuthServiceTest {

    @Test
    // @Disabled
    void getToken() {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        RemotePreferences remotePreferences = mock(RemotePreferences.class);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://localhost:23119"));
        OAuthSessionRegistry oAuthSessionRegistry = new OAuthSessionRegistry();
        CiteDriveOAuthService citeDriveOAuthService = new CiteDriveOAuthService(externalApplicationsPreferences, remotePreferences, oAuthSessionRegistry);
        citeDriveOAuthService.authorizeInteractive();
    }
}
