package org.jabref.logic.net;

/*
 import com.github.tomakehurst.wiremock.WireMockServer;
 import org.junit.jupiter.api.AfterEach;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;

 import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
 import static com.github.tomakehurst.wiremock.client.WireMock.any;
 import static com.github.tomakehurst.wiremock.client.WireMock.get;
 import static com.github.tomakehurst.wiremock.client.WireMock.head;
 import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
 import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
 import static org.junit.jupiter.api.Assertions.assertFalse;
 import static org.junit.jupiter.api.Assertions.assertTrue;

 class MimeTypeDetectorTest {

    private WireMockServer wireMockServer = new WireMockServer();

    @BeforeEach
    void before() {
        wireMockServer.start();
    }

    @AfterEach
    void after() {
        wireMockServer.stop();
    }

    @Test
    void handlePermanentRedirections() throws IOException {
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
    void beFalseForUnreachableUrl() throws IOException {
        String invalidUrl = "http://idontknowthisurlforsure.de";
        assertFalse(new URLDownload(invalidUrl).isMimeType("application/pdf"));
    }

    @Test
    void beTrueForPdfMimeType() throws IOException {
        String pdfUrl = "http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf";
        assertTrue(new URLDownload(pdfUrl).isMimeType("application/pdf"));
    }

    @Test
    void beTrueForLocalPdfUri() throws URISyntaxException, IOException {
        String localPath = MimeTypeDetectorTest.class.getResource("empty.pdf").toURI().toASCIIString();
        assertTrue(new URLDownload(localPath).isMimeType("application/pdf"));
    }

    @Test
    void beTrueForPDFMimeTypeVariations() throws IOException {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(any(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(new URLDownload(mimeTypeVariation).isMimeType("application/pdf"));
    }

    @Test
    void beAbleToUseHeadRequest() throws IOException {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(head(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(new URLDownload(mimeTypeVariation).isMimeType("application/pdf"));
    }

    @Test
    void beAbleToUseGetRequest() throws IOException {
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
*/
