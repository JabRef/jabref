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
package net.sf.jabref;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class JabRefCLITest {

    @Test
    public void testCLIParsingLongOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getFileImport());
        Assert.assertTrue(cli.isDisableGui());
        Assert.assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testCLIParsingShortOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getFileImport());
        Assert.assertTrue(cli.isDisableGui());
        Assert.assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testPreferencesExport() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-x=some/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getPreferencesExport());
        Assert.assertTrue(cli.isDisableGui());
    }

}