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
package net.sf.jabref.logic.net;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import net.sf.jabref.logic.net.URLDownload;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class URLDownloadTest {

    @Test
    public void testStringDownloadWithSetEncoding() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        Assert.assertTrue("google.com should contain google", dl.downloadToString("UTF8").contains("Google"));
    }

    @Test
    public void testStringDownload() throws IOException {
        Globals.prefs = JabRefPreferences.getInstance();
        try {
            URLDownload dl = new URLDownload(new URL("http://www.google.com"));

            Assert.assertTrue("google.com should contain google", dl.downloadToString().contains("Google"));
        } finally {
            Globals.prefs = null;
        }
    }

    @Test
    public void testFileDownload() throws IOException {
        File destination = File.createTempFile("jabref-test", ".html");
        try {
            URLDownload dl = new URLDownload(new URL("http://www.google.com"));
            dl.downloadToFile(destination);
            Assert.assertTrue("file must exist", destination.exists());
        } finally {
            // cleanup
            destination.delete();
        }
    }

    @Test
    public void testDetermineMimeType() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        Assert.assertTrue(dl.determineMimeType().startsWith("text/html"));
    }

}