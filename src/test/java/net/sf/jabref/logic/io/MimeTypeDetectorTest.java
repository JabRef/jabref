package net.sf.jabref.logic.io;

import org.junit.Test;
import org.junit.Ignore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

public class MimeTypeDetectorTest {
    @Test
    public void beFalseForInvalidUrl() {
        String invalidUrl = "thisisnourl";
        assertFalse(MimeTypeDetector.isPdfContentType(invalidUrl));
    }

    @Test
    public void beFalseForUnreachableUrl() {
        String invalidUrl = "http://idontknowthisurlforsure.de";
        assertFalse(MimeTypeDetector.isPdfContentType(invalidUrl));
    }

    @Test
    public void beTrueForPdfMimeType() {
        String pdfUrl = "http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf";
        assertTrue(MimeTypeDetector.isPdfContentType(pdfUrl));
    }

    @Test
    public void beTrueForLocalPdfUri() throws URISyntaxException {
        String localPath = MimeTypeDetectorTest.class.getResource("empty.pdf").toURI().toASCIIString();
        assertTrue(MimeTypeDetector.isPdfContentType(localPath));
    }

    @Ignore
    @Test
    public void acceptPDFMimeTypeVariations() {
        // application/pdf;charset=ISO-8859-1
        String pdfUrl = "http://drum.lib.umd.edu/bitstream/1903/19/2/CS-TR-3368.pdf";
        assertTrue(MimeTypeDetector.isPdfContentType(pdfUrl));
    }
}