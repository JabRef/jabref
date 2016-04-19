package net.sf.jabref.logic.io;

import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MimeTypeDetectorTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void handlePermanentRedirections() {
        String redirectedUrl = "http://localhost:8080/redirection";

        stubFor(get(urlEqualTo("/redirection"))
                .willReturn(
                        aResponse()
                                .withStatus(301)
                                .withHeader("Location", "http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf")
                )
        );

        assertTrue(MimeTypeDetector.isPdfContentType(redirectedUrl));
    }

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

    @Test
    public void beTrueForPDFMimeTypeVariations() {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(get(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(MimeTypeDetector.isPdfContentType(mimeTypeVariation));
    }
}