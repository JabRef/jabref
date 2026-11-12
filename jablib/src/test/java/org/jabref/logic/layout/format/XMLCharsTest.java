package org.jabref.logic.layout.format;

import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XMLCharsTest {

    private final LayoutFormatter formatter = new XMLChars();

    private static Stream<Arguments> format() {
        return Stream.of(
                Arguments.of("normal", "normal"),
                Arguments.of("&#233;", "é"),
                Arguments.of("a &lt; b &gt; c", "a < b > c"),
                Arguments.of("&#x0026;", "&"),
                Arguments.of("&#120572;", "𝛼"),
                Arguments.of("&#128512;", "😀"),
                Arguments.of("Smile &#128512; and &#233;", "Smile 😀 and é"));
    }

    @ParameterizedTest
    @MethodSource
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"normal", "é", "𝛼", "😀"})
    void formattedTextIsParsableXml(String input) throws ParserConfigurationException, SAXException, IOException {
        String xml = "<title>" + formatter.format(input) + "</title>";

        Document document = DocumentBuilderFactory.newInstance()
                                                  .newDocumentBuilder()
                                                  .parse(new InputSource(Reader.of(xml)));

        assertEquals(input, document.getDocumentElement().getTextContent());
    }
}
