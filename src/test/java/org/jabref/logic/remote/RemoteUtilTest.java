package org.jabref.logic.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteUtilTest {

    @Test
    void rejectPortNumberBelowZero() {
        assertFalse(RemoteUtil.isUserPort(-55), "Port number must be non negative.");
    }

    @Test
    void rejectReservedSystemPorts() {
        assertFalse(RemoteUtil.isUserPort(0), "Port number must be outside reserved system range (0-1023).");
        assertFalse(RemoteUtil.isUserPort(1023), "Port number must be outside reserved system range (0-1023).");
    }

    @Test
    void rejectPortsAbove16Bits() {
        // 2 ^ 16 - 1 => 65535
        assertFalse(RemoteUtil.isUserPort(65536), "Port number should be below 65535.");
    }

    @Test
    void acceptPortsAboveSystemPorts() {
        // ports 1024 -> 65535
        assertTrue(RemoteUtil.isUserPort(1024), "Port number in between 1024 and 65535 should be valid.");
        assertTrue(RemoteUtil.isUserPort(65535), "Port number in between 1024 and 65535 should be valid.");
    }
}
