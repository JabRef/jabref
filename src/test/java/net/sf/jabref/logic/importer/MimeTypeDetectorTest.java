/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.logic.importer;

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
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Test
    public void handlePermanentRedirections() {
        String redirectedUrl = "http://localhost:8080/redirection";

        stubFor(any(urlEqualTo("/redirection"))
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

        stubFor(any(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(MimeTypeDetector.isPdfContentType(mimeTypeVariation));
    }

    @Test
    public void beAbleToUseHeadRequest() {
        String mimeTypeVariation = "http://localhost:8080/mimevariation";

        stubFor(head(urlEqualTo("/mimevariation"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/pdf;charset=ISO-8859-1")
                )
        );

        assertTrue(MimeTypeDetector.isPdfContentType(mimeTypeVariation));
    }

    @Test
    public void beAbleToUseGetRequest() {
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

        assertTrue(MimeTypeDetector.isPdfContentType(mimeTypeVariation));
    }
}
