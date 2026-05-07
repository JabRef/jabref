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
    @CsvSource(
            textBlock = """
                    0
                    1023
                    """
    )
    void rejectReservedSystemPorts(int port) {
        assertFalse(RemoteUtil.isUserPort(port));
    }

    @Test
    void rejectPortsAbove16Bits() {
        // 2 ^ 16 - 1 => 65535
        assertFalse(RemoteUtil.isUserPort(65536), "Port number should be below 65535.");
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    # ports 1024 -> 65535
                    1024
                    65535
                    """
    )
    void acceptPortsAboveSystemPorts(int port) {
        assertTrue(RemoteUtil.isUserPort(port));
    }
}
