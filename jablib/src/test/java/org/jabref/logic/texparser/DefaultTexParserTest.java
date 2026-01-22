package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.texparser.LatexParserResults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultTexParserTest {

    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";

    private void testMatchCite(String key, int expectedStart, int expectedEnd, String citeString) {
        Path path = Path.of("");
        LatexParserResult latexParserResult = new DefaultLatexParser().parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(path);

        expectedParserResult.addKey(key, path, 1, expectedStart, expectedEnd, citeString);

        assertEquals(expectedParserResult, latexParserResult);
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    \\citet21312{123U123n123resolvedKey}
                    \\1cite[pr234e][post]{UnresolvedKey}
                    \\citep55{5}UnresolvedKey}
                    \\cit2et{UnresolvedKey}
                    """
    )
    void nonMatchCite(String citeString) {
        LatexParserResult latexParserResult = new DefaultLatexParser().parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(Path.of(""));

        assertEquals(expectedParserResult, latexParserResult);
    }

    private static Stream<Arguments> matchCiteCommandsProvider() {
        return Stream.of(
                Arguments.of(UNRESOLVED, 17, 30, "\\cite[pre][post]{UnresolvedKey}"),
                Arguments.of(UNRESOLVED, 7, 20, "\\cite*{UnresolvedKey}"),
                Arguments.of(UNRESOLVED, 17, 30, "\\parencite[post]{UnresolvedKey}"),
                Arguments.of(EINSTEIN_C, 7, 20, "\\citep{Einstein1920c}"),
                Arguments.of(EINSTEIN_C, 10, 23, "\\autocite{Einstein1920c}"),
                Arguments.of(EINSTEIN_C, 10, 23, "\\Autocite{Einstein1920c}"),
                Arguments.of(DARWIN, 20, 30, "\\blockcquote[p. 28]{Darwin1888}{some text}"),
                Arguments.of(DARWIN, 19, 29, "\\textcquote[p. 18]{Darwin1888}{blablabla}")
        );
    }

    @ParameterizedTest
    @MethodSource("matchCiteCommandsProvider")
    void matchCiteCommands(String expectedKey, int expectedStart, int expectedEnd, String citeString) {
        testMatchCite(expectedKey, expectedStart, expectedEnd, citeString);
    }

    @Test
    void twoCitationsSameLine() {
        Path path = Path.of("");
        String citeString = "\\citep{Einstein1920c} and \\citep{Einstein1920a}";

        LatexParserResult latexParserResult = new DefaultLatexParser().parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(path);

        expectedParserResult.addKey(EINSTEIN_C, path, 1, 7, 20, citeString);
        expectedParserResult.addKey(EINSTEIN_A, path, 1, 33, 46, citeString);

        assertEquals(expectedParserResult, latexParserResult);
    }

    @Test
    void fileEncodingUtf8() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("utf-8.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addKey("anykey", texFile, 1, 38, 44,
                "Danach wir anschließend mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void fileEncodingIso88591() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("iso-8859-1.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult.addKey("anykey", texFile, 1, 38, 44,
                "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void fileEncodingIso885915() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("iso-8859-15.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult.addKey("anykey", texFile, 1, 38, 44,
                "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void fileEncodingForThreeFiles() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("utf-8.tex").toURI());
        Path texFile2 = Path.of(DefaultTexParserTest.class.getResource("iso-8859-1.tex").toURI());
        Path texFile3 = Path.of(DefaultTexParserTest.class.getResource("iso-8859-15.tex").toURI());

        LatexParserResults parserResults = new DefaultLatexParser().parse(
                List.of(texFile, texFile2, texFile3));

        LatexParserResult result1 = new LatexParserResult(texFile);
        result1.addKey("anykey", texFile, 1, 38, 44, "Danach wir anschließend mittels \\cite{anykey}.");
        LatexParserResult result2 = new LatexParserResult(texFile2);
        result2.addKey("anykey", texFile2, 1, 38, 44,
                "Danach wir anschlie�end mittels \\cite{anykey}.");
        LatexParserResult result3 = new LatexParserResult(texFile3);
        result3.addKey("anykey", texFile3, 1, 38, 44,
                "Danach wir anschlie�end mittels \\cite{anykey}.");

        LatexParserResults expectedParserResults = new LatexParserResults(result1, result2, result3);

        assertEquals(expectedParserResults, parserResults);
    }

    @Test
    void singleFile() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 6, 18, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 6, 16, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 20, 32,
                "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 73, 83,
                "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void twoFiles() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Path.of(DefaultTexParserTest.class.getResource("paper2.tex").toURI());

        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile, texFile2));

        LatexParserResult result1 = new LatexParserResult(texFile);
        result1.addBibFile(texFile.getParent().resolve("origin.bib"));
        result1.addKey(EINSTEIN, texFile, 4, 6, 18, "\\cite{Einstein1920}");
        result1.addKey(DARWIN, texFile, 5, 6, 16, "\\cite{Darwin1888}.");
        result1.addKey(EINSTEIN, texFile, 6, 20, 32,
                "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        result1.addKey(DARWIN, texFile, 7, 73, 83,
                "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        LatexParserResult result2 = new LatexParserResult(texFile2);
        result2.addBibFile(texFile2.getParent().resolve("origin.bib"));
        result2.addKey(DARWIN, texFile2, 4, 54, 64,
                "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        result2.addKey(EINSTEIN, texFile2, 5, 54, 66,
                "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        result2.addKey(NEWTON, texFile2, 6, 54, 64,
                "This is some content trying to cite a bib file: \\cite{Newton1999}");

        LatexParserResults expectedParserResults = new LatexParserResults(result1, result2);

        assertEquals(expectedParserResults, parserResults);
    }

    @Test
    void duplicateFiles() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile, texFile));

        LatexParserResult result = new LatexParserResult(texFile);

        result.addBibFile(texFile.getParent().resolve("origin.bib"));
        result.addKey(EINSTEIN, texFile, 4, 6, 18, "\\cite{Einstein1920}");
        result.addKey(DARWIN, texFile, 5, 6, 16, "\\cite{Darwin1888}.");
        result.addKey(EINSTEIN, texFile, 6, 20, 32,
                "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        result.addKey(DARWIN, texFile, 7, 73, 83,
                "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        LatexParserResults expectedParserResults = new LatexParserResults(result, result);

        assertEquals(expectedParserResults, parserResults);
    }

    @Test
    void unknownKey() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("unknown_key.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(DARWIN, texFile, 4, 54, 64,
                "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile, 5, 54, 66,
                "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(UNKNOWN, texFile, 6, 54, 64,
                "This is some content trying to cite a bib file: \\cite{UnknownKey}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void fileNotFound() {
        Path texFile = Path.of("file_not_found.tex");
        Optional<LatexParserResult> parserResult = new DefaultLatexParser().parse(texFile);
        assertEquals(Optional.empty(), parserResult);
    }

    @Test
    void nestedFiles() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("nested.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile).get();
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addNestedFile(texFile.getParent().resolve("nested2.tex"));

        assertEquals(expectedParserResult, parserResult);
    }
}
