package org.jabref.logic.importer.util;

import java.io.StringReader;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaxParserTest {
    private static XMLInputFactory xmlInputFactory;

    @BeforeAll
    public static void setUp() {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    @ParameterizedTest
    @MethodSource("tests")
    void getsCompleteXMLContent(String expected, String input) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(input));
        assertEquals(expected, StaxParser.getXMLContent(reader));
    }

    private static Stream<Arguments> tests() {
        return Stream.of(
                Arguments.of("<ForeName xmlns=\"\" attr=\"1\">Alan</ForeName>",
                        """
                                <ForeName attr="1">Alan</ForeName>
                                """),
                Arguments.of("<ForeName xmlns=\"\" attr=\"1\">Alan</ForeName>",
                        """
                                <ForeName attr="1">Alan</ForeName>
                                <LastName attr="2">Grant</LastName>
                                """),
                Arguments.of("<ForeName xmlns=\"\" attr=\"1\">Alan<ForeName attr=\"5\">MiddleName</ForeName></ForeName>",
                        """
                                <ForeName attr="1">
                                    Alan
                                    <ForeName attr="5">MiddleName</ForeName>
                                </ForeName>
                                <LastName attr="2">Grant</LastName>
                                """),
                Arguments.of("<PubDate xmlns=\"\"><Year>2020</Year><Month>Jul</Month><Day>24</Day></PubDate>",
                        """
                                <PubDate>
                                    <Year>2020</Year>
                                    <Month>Jul</Month>
                                    <Day>24</Day>
                                </PubDate>
                                """),
                Arguments.of("<mml:math xmlns:mml=\"http://www.w3.org/1998/Math/MathML\"><mml:mrow><mml:msubsup><mml:mi>η</mml:mi><mml:mi>p</mml:mi><mml:mn>2</mml:mn></mml:msubsup></mml:mrow></mml:math>",
                        """
                                <mml:math xmlns:mml="http://www.w3.org/1998/Math/MathML">
                                    <mml:mrow>
                                        <mml:msubsup>
                                            <mml:mi>η</mml:mi>
                                            <mml:mi>p</mml:mi>
                                            <mml:mn>2</mml:mn>
                                        </mml:msubsup>
                                    </mml:mrow>
                                </mml:math>
                                """),
                Arguments.of("<Journal xmlns=\"\"><ISSN IssnType=\"Electronic\">1613-4516</ISSN><JournalIssue CitedMedium=\"Internet\"><Volume>17</Volume><Issue>2-3</Issue><PubDate><Year>2020</Year><Month>Jul</Month><Day>24</Day></PubDate></JournalIssue><Title>Journal of integrative bioinformatics</Title><ISOAbbreviation>J Integr Bioinform</ISOAbbreviation></Journal>",
                        """
                                <Journal>
                                    <ISSN IssnType="Electronic">1613-4516</ISSN>
                                    <JournalIssue CitedMedium="Internet">
                                        <Volume>17</Volume>
                                        <Issue>2-3</Issue>
                                        <PubDate>
                                            <Year>2020</Year>
                                            <Month>Jul</Month>
                                            <Day>24</Day>
                                        </PubDate>
                                    </JournalIssue>
                                    <Title>Journal of integrative bioinformatics</Title>
                                    <ISOAbbreviation>J Integr Bioinform</ISOAbbreviation>
                                </Journal>
                                """)
        );
    }
}
