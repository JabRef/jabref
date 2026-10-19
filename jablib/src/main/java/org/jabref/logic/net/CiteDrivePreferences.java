package org.jabref.logic.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteDrivePreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDrivePreferences.class);

    /// OAuth tokens
    ///
    /// - Refresh token (long-lived, confidential). -- could be unavailable; therefore "Optional"
    /// - Access token (short-lived) - not stored here
    private final BooleanProperty enabled;
    private final ObjectProperty<@Nullable RefreshToken> refreshToken;
    private final BooleanProperty persistRefreshToken;

    /// CiteDrive REST API (OAuth and push endpoints); the dev server until CiteDrive goes live
    private final StringProperty apiBaseUrl;
    /// CiteDrive web app, where the user completes an import
    private final StringProperty appBaseUrl;
    private long serverRevision;

    public CiteDrivePreferences(boolean enabled,
                                @Nullable RefreshToken refreshToken,
                                boolean persistRefreshToken,
                                String apiBaseUrl,
                                String appBaseUrl) {
        this.enabled = new SimpleBooleanProperty(enabled);
        this.refreshToken = new SimpleObjectProperty<>(refreshToken);
        this.persistRefreshToken = new SimpleBooleanProperty(persistRefreshToken);
        this.apiBaseUrl = new SimpleStringProperty(apiBaseUrl);
        this.appBaseUrl = new SimpleStringProperty(appBaseUrl);
        this.apiBaseUrl.addListener((_, _, _) -> invalidateLogin());
    }

    // Creates object with default preference values
    private CiteDrivePreferences() {
        this(
                false, // CiteDrive is opt-in
                null,  // no refresh token
                true, // store in keychain
                "https://api-dev.citedrive.com/",
                "https://app-dev.citedrive.com/"
        );
    }

    private synchronized void invalidateLogin() {
        serverRevision++;
        refreshToken.setValue(null);
    }

    public synchronized long getServerRevision() {
        return serverRevision;
    }

    /// A response from an earlier server configuration must not overwrite the current login.
    public synchronized boolean updateRefreshToken(long revision, Optional<RefreshToken> token) {
        if (revision != serverRevision) {
            return false;
        }
        token.ifPresentOrElse(this::setRefreshToken, () -> refreshToken.setValue(null));
        return true;
    }

    // [impl->req~citedrive.https~1]
    public static boolean isSecureUrl(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && Optional.ofNullable(uri.getHost()).filter(host -> !host.isBlank()).isPresent()
                    && Optional.ofNullable(uri.getRawUserInfo()).isEmpty();
        } catch (URISyntaxException e) {
            LOGGER.debug("Invalid CiteDrive address", e);
            return false;
        }
    }

    public static CiteDrivePreferences getDefault() {
        return new CiteDrivePreferences();
    }

    public synchronized void setAll(CiteDrivePreferences preferences) {
        this.enabled.set(preferences.isEnabled());
        this.persistRefreshToken.set(preferences.shouldPersistRefreshToken());
        this.apiBaseUrl.set(preferences.getApiBaseUrl());
        this.appBaseUrl.set(preferences.getAppBaseUrl());
        this.refreshToken.set(preferences.getRefreshToken());
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public synchronized String getApiBaseUrl() {
        return apiBaseUrl.get();
    }

    public StringProperty apiBaseUrlProperty() {
        return apiBaseUrl;
    }

    public synchronized void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl.set(apiBaseUrl);
    }

    public String getAppBaseUrl() {
        return appBaseUrl.get();
    }

    public StringProperty appBaseUrlProperty() {
        return appBaseUrl;
    }

    public void setAppBaseUrl(String appBaseUrl) {
        this.appBaseUrl.set(appBaseUrl);
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
        return URLUtil.createUri(getAppBaseUrl()).resolve("jabref/import/");
    }

    public final synchronized @Nullable RefreshToken getRefreshToken() {
        return refreshToken.get();
    }

    /// NOT called from the GUI preferences page, but from the OAuth flow handling components
    public synchronized void setRefreshToken(@Nullable RefreshToken refreshToken) {
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

    public ObservableValue<@Nullable RefreshToken> getRefreshTokenProperty() {
        return refreshToken;
    }
}
