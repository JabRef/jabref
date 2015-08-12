/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.remote;

import junit.framework.Assert;
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
