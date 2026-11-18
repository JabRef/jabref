package org.jabref.gui.preview;

import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewHtmlProcessorTest {

    @Test
    void processNullOrEmptyReturnsEmpty() {
        PreviewHtmlProcessor.Result nullResult = PreviewHtmlProcessor.process(null);
        assertEquals("", nullResult.html());
        assertEquals(Optional.empty(), nullResult.fontFamily());
        assertEquals(OptionalDouble.empty(), nullResult.fontSize());

        PreviewHtmlProcessor.Result emptyResult = PreviewHtmlProcessor.process("");
        assertEquals("", emptyResult.html());
        assertEquals(Optional.empty(), emptyResult.fontFamily());
        assertEquals(OptionalDouble.empty(), emptyResult.fontSize());
    }

    @Test
    void processExtractsFontPropertiesFromBodyStyleBlock() {
        String html = """
                <html>
                <head>
                <style>
                body {
                    font-family: "Times New Roman", serif;
                    font-size: 14pt;
                }
                </style>
                </head>
                <body>
                <p>Sample preview text</p>
                </body>
                </html>
                """;

        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertTrue(result.fontFamily().isPresent());
        assertEquals("Times New Roman", result.fontFamily().orElseThrow());
        assertTrue(result.fontSize().isPresent());
        assertEquals(14.0 * 96.0 / 72.0, result.fontSize().orElseThrow(), 0.001);
        assertFalse(result.html().contains("<style>"));
        assertTrue(result.html().contains("Sample preview text"));
    }

    @Test
    void processInlinesNonBodyCssRules() {
        String html = """
                <style>
                h1 {
                    font-family: Arial;
                    font-weight: bold;
                }
                .highlight {
                    color: blue;
                }
                </style>
                <h1>Article Title</h1>
                <p class="highlight">Some description</p>
                """;

        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertTrue(result.html().contains("style=\"font-family: Arial; font-weight: bold;\""));
        assertTrue(result.html().contains("style=\"color: blue;\""));
        assertFalse(result.html().contains("<style>"));
    }

    @Test
    void processExtractsFromInlineBodyStyle() {
        String html = """
                <body style="font-family: Georgia; font-size: 18px;">
                <p>Hello world</p>
                </body>
                """;

        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertTrue(result.fontFamily().isPresent());
        assertEquals("Georgia", result.fontFamily().orElseThrow());
        assertTrue(result.fontSize().isPresent());
        assertEquals(18.0, result.fontSize().orElseThrow(), 0.001);
    }

    @Test
    void processExtractsFromFontElement() {
        String html = """
                <font face="Verdana" size="4">
                <p>Content</p>
                </font>
                """;

        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertTrue(result.fontFamily().isPresent());
        assertEquals("Verdana", result.fontFamily().orElseThrow());
        assertTrue(result.fontSize().isPresent());
        assertEquals(18.0, result.fontSize().orElseThrow(), 0.001);
    }

    @Test
    void processParsesEmAndNamedSizes() {
        String emHtml = """
                <style>
                body {
                    font-size: 1.5em;
                }
                </style>
                <p>Text</p>
                """;
        PreviewHtmlProcessor.Result emResult = PreviewHtmlProcessor.process(emHtml);
        assertTrue(emResult.fontSize().isPresent());
        assertEquals(24.0, emResult.fontSize().orElseThrow(), 0.001);

        String namedHtml = """
                <style>
                body {
                    font-size: small;
                }
                </style>
                <p>Text</p>
                """;
        PreviewHtmlProcessor.Result namedResult = PreviewHtmlProcessor.process(namedHtml);
        assertTrue(namedResult.fontSize().isPresent());
        assertEquals(13.0, namedResult.fontSize().orElseThrow(), 0.001);
    }

    @Test
    void processConvertsSuperscriptsAndSubscriptsToUnicode() {
        String html = "<p>LiNbO<sub>3</sub>:Er<sup>3+</sup>/Yb<sup>3+</sup></p>";
        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertEquals("<p>LiNbO₃:Er³⁺/Yb³⁺</p>", result.html());
    }

    @Test
    void processConvertsOrdinalsAndFormulas() {
        String html = "<p>1<sup>st</sup>, 2<sup>nd</sup>, 3<sup>rd</sup>, 4<sup>th</sup>, x<sup>2</sup>, and H<sub>2</sub>O</p>";
        PreviewHtmlProcessor.Result result = PreviewHtmlProcessor.process(html);
        assertEquals("<p>1ˢᵗ, 2ⁿᵈ, 3ʳᵈ, 4ᵗʰ, x², and H₂O</p>", result.html());
    }
}
