package org.jabref.logic.remote.server;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.jabref.logic.remote.RemotePreferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages PIN-based pairing and bearer token authentication for the HTTP server.
///
/// PINs are ephemeral (in-memory only, with TTL). Tokens are persisted via RemotePreferences.
@NullMarked
public class ConnectorTokenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorTokenManager.class);

    private static final int TOKEN_BYTES = 32;
    private static final long PIN_TTL_SECONDS = 300;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RemotePreferences remotePreferences;

    private String activePin = "";
    private Instant pinExpiration = Instant.MIN;

    public ConnectorTokenManager(RemotePreferences remotePreferences) {
        this.remotePreferences = remotePreferences;
    }

    /// Generates a new 6-digit PIN valid for 5 minutes. Invalidates any previous PIN.
    public String generatePin() {
        int pinValue = secureRandom.nextInt(1_000_000);
        activePin = String.format("%06d", pinValue);
        pinExpiration = Instant.now().plusSeconds(PIN_TTL_SECONDS);
        return activePin;
    }

    /// Validates the given PIN and returns a token for the client.
    /// If a token already exists, it is reused (allowing multiple extensions to share the same token).
    /// The PIN is invalidated after use (one-time).
    ///
    /// @return the token if PIN was valid, empty otherwise
    public Optional<String> validatePinAndGenerateToken(String pin) {
        if (activePin.isEmpty() || Instant.now().isAfter(pinExpiration)) {
            LOGGER.debug("PIN validation failed: no active PIN or expired");
            invalidatePin();
            return Optional.empty();
        }

        if (!activePin.equals(pin)) {
            LOGGER.debug("PIN validation failed: incorrect PIN");
            return Optional.empty();
        }

        invalidatePin();
        String existingToken = remotePreferences.getApiToken();
        if (!existingToken.isBlank()) {
            return Optional.of(existingToken);
        }
        String token = generateToken();
        remotePreferences.setApiToken(token);
        return Optional.of(token);
    }

    /// Checks whether the provided token matches the stored token.
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String storedToken = remotePreferences.getApiToken();
        return !storedToken.isBlank() && storedToken.equals(token);
    }

    /// Revokes the stored token.
    public void revokeToken() {
        remotePreferences.setApiToken("");
    }

    /// Returns the currently active PIN for display in the GUI.
    public Optional<String> getActivePin() {
        if (activePin.isEmpty() || Instant.now().isAfter(pinExpiration)) {
            invalidatePin();
            return Optional.empty();
        }
        return Optional.of(activePin);
    }

    /// Returns whether a valid (non-expired) token is stored.
    public boolean hasActiveToken() {
        return !remotePreferences.getApiToken().isBlank();
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void invalidatePin() {
        activePin = "";
        pinExpiration = Instant.MIN;
    }
}
