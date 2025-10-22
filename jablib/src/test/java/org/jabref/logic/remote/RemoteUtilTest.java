package org.jabref.logic.remote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteUtilTest {

    @Test
    void rejectPortNumberBelowZero() {
        assertFalse(RemoteUtil.isUserPort(-55), "Port number must be non negative.");
    }

    @ParameterizedTest
    @CsvSource({
            "0",
            "1023"
    })
    void rejectReservedSystemPorts(int port) {
        String message = "Port number must be outside reserved system range (0-1023).";
        assertFalse(RemoteUtil.isUserPort(port), message);
    }

    @Test
    void rejectPortsAbove16Bits() {
        // 2 ^ 16 - 1 => 65535
        assertFalse(RemoteUtil.isUserPort(65536), "Port number should be below 65535.");
    }

    @ParameterizedTest
    @CsvSource({
            // ports 1024 -> 65535
            "1024",
            "65535"
    })
    void acceptPortsAboveSystemPorts(int port) {
        String message = "Port number in between 1024 and 65535 should be valid.";
        assertTrue(RemoteUtil.isUserPort(port), message);
    }
}
