package org.jabref.logic.citedrive;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.remote.RemotePreferences;

import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->req~citedrive.login~1]
// [utest->req~citedrive.push~1]
class CiteDriveOAuthServiceTest {

    // Nothing listens on port 1, so every token request fails with a connection error
    private static final URI UNREACHABLE = URI.create("http://127.0.0.1:1/");

    private final RemotePreferences remotePreferences = mock(RemotePreferences.class);

    @Test
    void disabledHttpServerFailsWithoutOpeningBrowser() {
        when(remotePreferences.shouldEnableHttpServer()).thenReturn(false);
        CiteDrivePreferences citeDrivePreferences = CiteDrivePreferences.getDefault();
        CiteDriveOAuthService service = new CiteDriveOAuthService(remotePreferences, citeDrivePreferences, new OAuthSessionRegistry(),
                _ -> {
                    throw new AssertionError("Browser must not be opened");
                },
                () -> false,
                UNREACHABLE, UNREACHABLE);

        assertThrows(ExecutionException.class, () -> service.authorizeInteractive().get(5, TimeUnit.SECONDS));
    }

    @Test
    void networkErrorOnRefreshKeepsStoredLogin() {
        RefreshToken refreshToken = new RefreshToken("stored");
        CiteDrivePreferences citeDrivePreferences = CiteDrivePreferences.getDefault();
        citeDrivePreferences.setRefreshToken(refreshToken);
        CiteDriveOAuthService service = new CiteDriveOAuthService(remotePreferences, citeDrivePreferences, new OAuthSessionRegistry(),
                _ -> {
                    throw new AssertionError("Browser must not be opened");
                },
                () -> false,
                UNREACHABLE, UNREACHABLE);

        assertThrows(ExecutionException.class, () -> service.getAccessToken().get(30, TimeUnit.SECONDS));
        assertEquals(refreshToken, citeDrivePreferences.getRefreshToken());
    }
}
