package net.sf.jabref.logic.remote;

import org.junit.Assert;
import org.junit.Test;

public class RemoteUtilTest {
    @Test
    public void rejectPortNumberBelowZero() {
        Assert.assertFalse("Port number must be non negative.", RemoteUtil.isUserPort(-55));
    }

    @Test
    public void rejectReservedSystemPorts() {
        Assert.assertFalse("Port number must be outside reserved system range (0-1023).", RemoteUtil.isUserPort(0));
        Assert.assertFalse("Port number must be outside reserved system range (0-1023).", RemoteUtil.isUserPort(1023));
    }

    @Test
    public void rejectPortsAbove16Bits() {
        // 2 ^ 16 - 1 => 65535
        Assert.assertFalse("Port number should be below 65535.", RemoteUtil.isUserPort(65536));
    }

    @Test
    public void acceptPortsAboveSystemPorts() {
        // ports 1024 -> 65535
        Assert.assertTrue("Port number in between 1024 and 65535 should be valid.", RemoteUtil.isUserPort(1024));
        Assert.assertTrue("Port number in between 1024 and 65535 should be valid.", RemoteUtil.isUserPort(65535));
    }
}
