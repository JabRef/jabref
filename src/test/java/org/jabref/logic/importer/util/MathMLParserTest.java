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

class MathMLParserTest {
    private static XMLInputFactory xmlInputFactory;

    @BeforeAll
    public static void setUp() {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    @ParameterizedTest
    @MethodSource("tests")
    void parserConvertsMathMLIntoLatex(String expected, String input) throws XMLStreamException {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(input));
        assertEquals(expected, MathMLParser.parse(reader));
    }

    private static Stream<Arguments> tests() {
        return Stream.of(
                Arguments.of("$\\begin{pmatrix}0 & 1 & 0\\\\ 0 & 0 & 1\\\\ 1 & 0 & 0\\end{pmatrix}$",
                        """
                                <math xmlns="http://www.w3.org/1998/Math/MathML">
                                    <matrix>
                                        <matrixrow>
                                            <cn> 0 </cn> <cn> 1 </cn> <cn> 0 </cn>
                                        </matrixrow>
                                        <matrixrow>
                                            <cn> 0 </cn> <cn> 0 </cn> <cn> 1 </cn>
                                        </matrixrow>
                                        <matrixrow>
                                            <cn> 1 </cn> <cn> 0 </cn> <cn> 0 </cn>
                                        </matrixrow>
                                    </matrix>
                                </math>
                                """),
                Arguments.of("${\\eta }_{p}^{2}$",
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
                Arguments.of("<Unsupported MathML expression>",
                        """
                                <mml:math xmlns=http://www.w3.org/1998/Math/MathML>
                                    <mml:mrow>
                                        <mml:msubsup>
                                            <mml:mi>η</abc>
                                        </mml:msubsup>
                                    </mml:mrow>
                                </mml:math>
                                """),
                Arguments.of("$\\underset{a}{\\overset{b}{\\int }}\\left(5x+2\\mathrm{sin}\\left(x\\right)\\right)\\mathrm{dx}$",
                        """
                                <math xmlns="http://www.w3.org/1998/Math/MathML">
                                    <munderover>
                                        <mo>&#x222B;</mo>
                                        <mi>a</mi>
                                        <mi>b</mi>
                                    </munderover>
                                    <mfenced separators=''>
                                        <mn>5</mn>
                                        <mi>x</mi>
                                        <mo>+</mo>
                                        <mn>2</mn>
                                        <mi>sin</mi>
                                        <mfenced separators=''>
                                            <mi>x</mi>
                                        </mfenced>
                                    </mfenced>
                                    <mi>dx</mi>
                                </math>
                                """),
                Arguments.of("$\\stackrel{\\to }{v}=\\left({v}_{1},{v}_{2},{v}_{3}\\right)$",
                        """
                                <math xmlns="http://www.w3.org/1998/Math/MathML">
                                    <mover>
                                        <mi>v</mi>
                                        <mo>&#x2192;</mo>
                                    </mover>
                                    <mo>=</mo>
                                    <mfenced>
                                        <msub>
                                            <mi>v</mi>
                                            <mn>1</mn>
                                        </msub>
                                        <msub>
                                            <mi>v</mi>
                                            <mn>2</mn>
                                        </msub>
                                        <msub>
                                            <mi>v</mi>
                                            <mn>3</mn>
                                        </msub>
                                    </mfenced>
                                </math>
                                """)
        );
    }
}
