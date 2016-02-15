package net.sf.jabref.logic.io;

import net.sf.jabref.support.DevEnvironment;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Ignore
    @Test
    public void acceptPDFMimeTypeVariations() {
        // application/pdf;charset=ISO-8859-1
        String pdfUrl = "http://drum.lib.umd.edu/bitstream/1903/19/2/CS-TR-3368.pdf";
        assertTrue(MimeTypeDetector.isPdfContentType(pdfUrl));
    }

    @Ignore
    @Test
    public void useGetRequestIfHeadRequestHasNoContentType() {
        String pdfUrl = "http://iopscience.iop.org/article/10.1088/1749-4699/8/1/014010/pdf";
        assertEquals("application/pdf", MimeTypeDetector.getMimeType(pdfUrl));
    }
}