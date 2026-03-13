package org.jabref.logic.remote.server;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.remote.RemotePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorTokenManagerTest {

    private RemotePreferences remotePreferences;
    private ConnectorTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        remotePreferences = new RemotePreferences(6050, true, 23119, false, false, 2087, List.of(), "");
        tokenManager = new ConnectorTokenManager(remotePreferences);
    }

    @Test
    void generatePinReturnsSixDigitString() {
        String pin = tokenManager.generatePin();
        assertEquals(6, pin.length());
        assertTrue(pin.matches("\\d{6}"));
    }

    @Test
    void validPinGeneratesToken() {
        String pin = tokenManager.generatePin();
        Optional<String> token = tokenManager.validatePinAndGenerateToken(pin);
        assertTrue(token.isPresent());
        assertFalse(token.get().isBlank());
    }

    @Test
    void tokenIsStoredInPreferences() {
        String pin = tokenManager.generatePin();
        String token = tokenManager.validatePinAndGenerateToken(pin).orElseThrow();
        assertEquals(token, remotePreferences.getApiToken());
    }

    @Test
    void pinIsInvalidatedAfterUse() {
        String pin = tokenManager.generatePin();
        tokenManager.validatePinAndGenerateToken(pin);
        Optional<String> secondAttempt = tokenManager.validatePinAndGenerateToken(pin);
        assertTrue(secondAttempt.isEmpty());
    }

    @Test
    void wrongPinIsRejected() {
        tokenManager.generatePin();
        Optional<String> token = tokenManager.validatePinAndGenerateToken("000000");
        assertTrue(token.isEmpty());
    }

    @Test
    void validateTokenWithCorrectToken() {
        String pin = tokenManager.generatePin();
        String token = tokenManager.validatePinAndGenerateToken(pin).orElseThrow();
        assertTrue(tokenManager.validateToken(token));
    }

    @Test
    void validateTokenWithWrongToken() {
        String pin = tokenManager.generatePin();
        tokenManager.validatePinAndGenerateToken(pin);
        assertFalse(tokenManager.validateToken("wrong-token"));
    }

    @Test
    void revokeTokenClearsStoredToken() {
        String pin = tokenManager.generatePin();
        tokenManager.validatePinAndGenerateToken(pin);
        assertTrue(tokenManager.hasActiveToken());

        tokenManager.revokeToken();
        assertFalse(tokenManager.hasActiveToken());
        assertEquals("", remotePreferences.getApiToken());
    }

    @Test
    void getActivePinReturnsEmptyWhenNoPin() {
        assertTrue(tokenManager.getActivePin().isEmpty());
    }

    @Test
    void getActivePinReturnsPinAfterGeneration() {
        String pin = tokenManager.generatePin();
        Optional<String> activePin = tokenManager.getActivePin();
        assertTrue(activePin.isPresent());
        assertEquals(pin, activePin.get());
    }

    @Test
    void newPinInvalidatesPreviousPin() {
        String pin1 = tokenManager.generatePin();
        String pin2 = tokenManager.generatePin();
        assertNotEquals(pin1, pin2);

        Optional<String> result = tokenManager.validatePinAndGenerateToken(pin1);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateTokenWithBlankTokenReturnsFalse() {
        assertFalse(tokenManager.validateToken(""));
        assertFalse(tokenManager.validateToken(null));
    }

    @Test
    void rePairingReusesExistingToken() {
        String pin1 = tokenManager.generatePin();
        String token1 = tokenManager.validatePinAndGenerateToken(pin1).orElseThrow();

        String pin2 = tokenManager.generatePin();
        String token2 = tokenManager.validatePinAndGenerateToken(pin2).orElseThrow();

        assertEquals(token1, token2);
    }

    @Test
    void pairingAfterRevokeGeneratesNewToken() {
        String pin1 = tokenManager.generatePin();
        String token1 = tokenManager.validatePinAndGenerateToken(pin1).orElseThrow();

        tokenManager.revokeToken();

        String pin2 = tokenManager.generatePin();
        String token2 = tokenManager.validatePinAndGenerateToken(pin2).orElseThrow();

        assertNotEquals(token1, token2);
    }
}
