package org.jabref.logic.net;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

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

    public CiteDrivePreferences(@Nullable RefreshToken refreshToken,
                                boolean persistRefreshToken) {
        this.refreshToken = new SimpleObjectProperty<>(refreshToken);
        this.persistRefreshToken = new SimpleBooleanProperty(persistRefreshToken);
    }

    // Creates object with default preference values
    private CiteDrivePreferences() {
        this(
                null,  // no refresh token
                true // store in keychain
        );
    }

    public static CiteDrivePreferences getDefault() {
        return new CiteDrivePreferences();
    }

    public void setAll(CiteDrivePreferences preferences) {
        this.refreshToken.set(preferences.getRefreshToken());
        this.persistRefreshToken.set(preferences.shouldPersistRefreshToken());
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
