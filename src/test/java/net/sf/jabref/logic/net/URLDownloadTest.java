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