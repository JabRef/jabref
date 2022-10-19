package org.jabref.logic.tele;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jabref.logic.tele.TeleUtil;

public class TeleUtilTest {

    @Test
    public void rejectPortNumberBelowZero() {
        assertFalse(TeleUtil.isUserPort(-55), "Port number must be non negative.");
    }

    @Test
    public void rejectReservedSystemPorts() {
        assertFalse(TeleUtil.isUserPort(0), "Port number must be outside reserved system range (0-1023).");
        assertFalse(TeleUtil.isUserPort(1023), "Port number must be outside reserved system range (0-1023).");
    }

    @Test
    public void rejectPortsAbove16Bits() {
        // 2 ^ 16 - 1 => 65535
        assertFalse(TeleUtil.isUserPort(65536), "Port number should be below 65535.");
    }

    @Test
    public void acceptPortsAboveSystemPorts() {
        // ports 1024 -> 65535
        assertTrue(TeleUtil.isUserPort(1024), "Port number in between 1024 and 65535 should be valid.");
        assertTrue(TeleUtil.isUserPort(65535), "Port number in between 1024 and 65535 should be valid.");
    }
}
