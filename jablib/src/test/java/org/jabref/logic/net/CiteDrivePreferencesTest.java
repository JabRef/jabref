package org.jabref.logic.net;

import java.util.Optional;

import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@NullMarked
class CiteDrivePreferencesTest {

    @ParameterizedTest
    @CsvSource({
            "https://api.citedrive.com/,true",
            "HTTPS://api.citedrive.com/,true",
            "http://api.citedrive.com/,false",
            "http://127.0.0.1:8080/,false",
            "https:///missing-host,false",
            "https://user:password@api.citedrive.com/,false",
            "file:///tmp/token,false",
            "'not a URL',false"
    })
    void serverAddressRequiresHttps(String address, boolean valid) {
        // [utest->req~citedrive.https~1]
        assertEquals(valid, CiteDrivePreferences.isSecureUrl(address));
    }

    @Test
    void changingServerInvalidatesLoginEvenWhenReturningToOriginalAddress() {
        // [utest->req~citedrive.credentials.server~1]
        CiteDrivePreferences preferences = CiteDrivePreferences.getDefault();
        long revision = preferences.getServerRevision();
        String originalAddress = preferences.getApiBaseUrl();
        preferences.setRefreshToken(new RefreshToken("original"));

        preferences.setApiBaseUrl("https://other.example/");
        preferences.setApiBaseUrl(originalAddress);

        assertFalse(preferences.updateRefreshToken(revision, Optional.of(new RefreshToken("stale"))));
        assertEquals(Optional.empty(), Optional.ofNullable(preferences.getRefreshToken()));
    }

    @Test
    void importingPreferencesKeepsTheLoginForTheImportedServer() {
        CiteDrivePreferences preferences = CiteDrivePreferences.getDefault();
        RefreshToken importedToken = new RefreshToken("imported");
        CiteDrivePreferences imported = new CiteDrivePreferences(true, importedToken, true, "https://other.example/", "https://app.example/");

        preferences.setAll(imported);

        assertEquals(importedToken, preferences.getRefreshToken());
    }
}
