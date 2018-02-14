package org.jabref.logic.net;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MimeTypeDetectorTest {

    @Rule public WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void handlePermanentRedirections() throws IOException {
        String redirectedUrl = "http://localhost:8080/redirection";

        stubFor(any(urlEqualTo("/redirection"))
                .willReturn(
                        aResponse()
                                .withStatus(301)
                                .withHeader("Location", "http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf")
                )
        );

        assertTrue(new URLDownload(redirectedUrl).isMimeType("application/pdf"));
    }

    @Test
    public void beFalseForUnreachableUrl() throws IOException {
        String invalidUrl = "http://idontknowthisurlforsure.de";
        assertFalse(new URLDownload(invalidUrl).isMimeType("application/pdf"));
    }

    @Test
    public void beTrueForPdfMimeType() throws IOException {
        String pdfUrl = "http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf";
        assertTrue(new URLDownload(pdfUrl).isMimeType("application/pdf"));
    }

    @Test
    public void beTrueForLocalPdfUri() throws URISyntaxException, IOException {
        String localPath = MimeTypeDetectorTest.class.getResource("empty.pdf").toURI().toASCIIString();
        assertTrue(new URLDownload(localPath).isMimeType("application/pdf"));
    }

    @Test
    public void beTrueForPDFMimeTypeVariations() throws IOException {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(any(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(new URLDownload(mimeTypeVariation).isMimeType("application/pdf"));
    }

    @Test
    public void beAbleToUseHeadRequest() throws IOException {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(head(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(new URLDownload(mimeTypeVariation).isMimeType("application/pdf"));
    }

    @Test
    public void beAbleToUseGetRequest() throws IOException {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(head(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withStatus(404)
                )
        );
        stubFor(get(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(new URLDownload(mimeTypeVariation).isMimeType("application/pdf"));
    }
}
