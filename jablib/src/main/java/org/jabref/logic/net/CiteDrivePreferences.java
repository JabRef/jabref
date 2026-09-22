package org.jabref.logic.net;

import java.net.URI;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.logic.util.URLUtil;

import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CiteDrivePreferences {

    /// OAuth tokens
    ///
    /// - Refresh token (long-lived, confidential). -- could be unavailable; therefore "Optional"
    /// - Access token (short-lived) - not stored here
    private final ObjectProperty<RefreshToken> refreshToken;
    private final BooleanProperty persistRefreshToken;

    /// CiteDrive REST API (OAuth and push endpoints); the dev server until CiteDrive goes live
    private final StringProperty apiBaseUrl;
    /// CiteDrive web app, where the user completes an import
    private final StringProperty appBaseUrl;

    public CiteDrivePreferences(@Nullable RefreshToken refreshToken,
                                boolean persistRefreshToken,
                                String apiBaseUrl,
                                String appBaseUrl) {
        this.refreshToken = new SimpleObjectProperty<>(refreshToken);
        this.persistRefreshToken = new SimpleBooleanProperty(persistRefreshToken);
        this.apiBaseUrl = new SimpleStringProperty(apiBaseUrl);
        this.appBaseUrl = new SimpleStringProperty(appBaseUrl);
    }

    // Creates object with default preference values
    private CiteDrivePreferences() {
        this(
                null,  // no refresh token
                true, // store in keychain
                "https://api-dev.citedrive.com/",
                "https://app-dev.citedrive.com/"
        );
    }

    public static CiteDrivePreferences getDefault() {
        return new CiteDrivePreferences();
    }

    public void setAll(CiteDrivePreferences preferences) {
        this.refreshToken.set(preferences.getRefreshToken());
        this.persistRefreshToken.set(preferences.shouldPersistRefreshToken());
        this.apiBaseUrl.set(preferences.getApiBaseUrl());
        this.appBaseUrl.set(preferences.getAppBaseUrl());
    }

    public String getApiBaseUrl() {
        return apiBaseUrl.get();
    }

    public StringProperty apiBaseUrlProperty() {
        return apiBaseUrl;
    }

    public String getAppBaseUrl() {
        return appBaseUrl.get();
    }

    public StringProperty appBaseUrlProperty() {
        return appBaseUrl;
    }

    public URI getAuthorizationEndpoint() {
        return URLUtil.createUri(getApiBaseUrl()).resolve("jabref/login/");
    }

    public URI getTokenEndpoint() {
        return URLUtil.createUri(getApiBaseUrl()).resolve("o/token/");
    }

    public URI getPushEndpoint() {
        return URLUtil.createUri(getApiBaseUrl()).resolve("jabref/push/");
    }

    /// Web page where the user picks which of the pushed entries to import into a CiteDrive project
    public URI getImportPage() {
        return URLUtil.createUri(getAppBaseUrl()).resolve("jabref/push/");
    }

    public final @Nullable RefreshToken getRefreshToken() {
        return refreshToken.get();
    }

    /// NOT called from the GUI preferences page, but from the OAuth flow handling components
    public void setRefreshToken(@Nullable RefreshToken refreshToken) {
        this.refreshToken.set(refreshToken);
    }

    public final boolean shouldPersistRefreshToken() {
        return persistRefreshToken.get();
    }

    public BooleanProperty persistRefreshTokenProperty() {
        return persistRefreshToken;
    }

    public void setPersistRefreshToken(boolean persistRefreshToken) {
        this.persistRefreshToken.set(persistRefreshToken);
    }

    public ObservableValue<RefreshToken> getRefreshTokenProperty() {
        return refreshToken;
    }
}
