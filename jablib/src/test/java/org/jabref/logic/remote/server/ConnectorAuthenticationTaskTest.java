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

class ConnectorAuthenticationTaskTest {

    private RemotePreferences remotePreferences;
    private ConnectorAuthenticationTask authenticationTask;

    @BeforeEach
    void setUp() {
        remotePreferences = new RemotePreferences(6050, true, 23119, false, false, 2087, List.of(), "", false);
        authenticationTask = new ConnectorAuthenticationTask(remotePreferences);
    }

    @Test
    void generatePinReturnsSixDigitString() {
        String pin = authenticationTask.generatePin();
        assertEquals(6, pin.length());
        assertTrue(pin.matches("\\d{6}"));
    }

    @Test
    void validPinGeneratesToken() {
        String pin = authenticationTask.generatePin();
        Optional<String> token = authenticationTask.validatePinAndGenerateToken(pin);
        assertTrue(token.isPresent());
        assertFalse(token.get().isBlank());
    }

    @Test
    void tokenIsStoredInPreferences() {
        String pin = authenticationTask.generatePin();
        String token = authenticationTask.validatePinAndGenerateToken(pin).orElseThrow();
        assertEquals(token, remotePreferences.getApiToken());
    }

    @Test
    void pinIsInvalidatedAfterUse() {
        String pin = authenticationTask.generatePin();
        authenticationTask.validatePinAndGenerateToken(pin);
        Optional<String> secondAttempt = authenticationTask.validatePinAndGenerateToken(pin);
        assertTrue(secondAttempt.isEmpty());
    }

    @Test
    void wrongPinIsRejected() {
        authenticationTask.generatePin();
        Optional<String> token = authenticationTask.validatePinAndGenerateToken("000000");
        assertTrue(token.isEmpty());
    }

    @Test
    void validateTokenWithCorrectToken() {
        String pin = authenticationTask.generatePin();
        String token = authenticationTask.validatePinAndGenerateToken(pin).orElseThrow();
        assertTrue(authenticationTask.validateToken(token));
    }

    @Test
    void validateTokenWithWrongToken() {
        String pin = authenticationTask.generatePin();
        authenticationTask.validatePinAndGenerateToken(pin);
        assertFalse(authenticationTask.validateToken("wrong-token"));
    }

    @Test
    void revokeTokenClearsStoredToken() {
        String pin = authenticationTask.generatePin();
        authenticationTask.validatePinAndGenerateToken(pin);
        assertTrue(authenticationTask.hasActiveToken());

        authenticationTask.revokeToken();
        assertFalse(authenticationTask.hasActiveToken());
        assertEquals("", remotePreferences.getApiToken());
    }

    @Test
    void getActivePinReturnsEmptyWhenNoPin() {
        assertTrue(authenticationTask.getActivePin().isEmpty());
    }

    @Test
    void getActivePinReturnsPinAfterGeneration() {
        String pin = authenticationTask.generatePin();
        Optional<String> activePin = authenticationTask.getActivePin();
        assertTrue(activePin.isPresent());
        assertEquals(pin, activePin.get());
    }

    @Test
    void newPinInvalidatesPreviousPin() {
        String pin1 = authenticationTask.generatePin();
        String pin2 = authenticationTask.generatePin();
        assertNotEquals(pin1, pin2);

        Optional<String> result = authenticationTask.validatePinAndGenerateToken(pin1);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateTokenWithBlankTokenReturnsFalse() {
        assertFalse(authenticationTask.validateToken(""));
        assertFalse(authenticationTask.validateToken(null));
    }

    @Test
    void rePairingReusesExistingToken() {
        String pin1 = authenticationTask.generatePin();
        String token1 = authenticationTask.validatePinAndGenerateToken(pin1).orElseThrow();

        String pin2 = authenticationTask.generatePin();
        String token2 = authenticationTask.validatePinAndGenerateToken(pin2).orElseThrow();

        assertEquals(token1, token2);
    }

    @Test
    void pairingAfterRevokeGeneratesNewToken() {
        String pin1 = authenticationTask.generatePin();
        String token1 = authenticationTask.validatePinAndGenerateToken(pin1).orElseThrow();

        authenticationTask.revokeToken();

        String pin2 = authenticationTask.generatePin();
        String token2 = authenticationTask.validatePinAndGenerateToken(pin2).orElseThrow();

        assertNotEquals(token1, token2);
    }
}
