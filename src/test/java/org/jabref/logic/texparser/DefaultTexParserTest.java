package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;

import org.jabref.model.texparser.LatexParserResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DefaultTexParserTest {

    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";
    private final static DefaultLatexParser LATEX_PARSER = new DefaultLatexParser();

    private void testMatchCite(String key, String citeString) {
        Path path = Path.of("");
        LatexParserResult latexParserResult = LATEX_PARSER.parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(path);

        expectedParserResult.addKey(key, path, 1, 0, citeString.length(), citeString);

        assertEquals(expectedParserResult, latexParserResult);
    }

    private void testNonMatchCite(String citeString) {
        LatexParserResult latexParserResult = LATEX_PARSER.parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(Path.of(""));

        assertEquals(expectedParserResult, latexParserResult);
    }

    @Test
    public void citeCommands() {
        testMatchCite(UNRESOLVED, "\\cite[pre][post]{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\cite*{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\parencite[post]{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\cite[pre][post]{UnresolvedKey}");
        testMatchCite(EINSTEIN_C, "\\citep{Einstein1920c}");
        testMatchCite(EINSTEIN_C, "\\autocite{Einstein1920c}");
        testMatchCite(EINSTEIN_C, "\\Autocite{Einstein1920c}");
        testMatchCite(DARWIN, "\\blockcquote[p. 28]{Darwin1888}{some text}");
        testMatchCite(DARWIN, "\\textcquote[p. 18]{Darwin1888}{blablabla}");

        testNonMatchCite("\\citet21312{123U123n123resolvedKey}");
        testNonMatchCite("\\1cite[pr234e][post]{UnresolvedKey}");
        testNonMatchCite("\\citep55{5}UnresolvedKey}");
        testNonMatchCite("\\cit2et{UnresolvedKey}");
    }

    @Test
    public void twoCitationsSameLine() {
        Path path = Path.of("");
        String citeString = "\\citep{Einstein1920c} and \\citep{Einstein1920a}";

        LatexParserResult latexParserResult = LATEX_PARSER.parse(citeString);
        LatexParserResult expectedParserResult = new LatexParserResult(path);

        expectedParserResult.addKey(EINSTEIN_C, path, 1, 0, 21, citeString);
        expectedParserResult.addKey(EINSTEIN_A, path, 1, 26, 47, citeString);

        assertEquals(expectedParserResult, latexParserResult);
    }

    @Test
    public void fileEncodingUtf8() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("utf-8.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addKey("anykey", texFile, 1, 32, 45, "Danach wir anschließend mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void fileEncodingIso88591() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("iso-8859-1.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult.addKey("anykey", texFile, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void fileEncodingIso885915() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("iso-8859-15.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult.addKey("anykey", texFile, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void singleFile() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);

        texFile = Path.of(DefaultTexParserTest.class.getResource("paper2.tex").toURI());
        parserResult = LATEX_PARSER.parse(texFile);

        expectedParserResult = new LatexParserResult(texFile);
        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(DARWIN, texFile, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(NEWTON, texFile, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{Newton1999}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void unknownKey() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("unknown_key.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(DARWIN, texFile, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(UNKNOWN, texFile, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{UnknownKey}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void fileNotFound() {
        Path texFile = Path.of("file_not_found.tex");
        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        assertNull(parserResult);
    }

    @Test
    public void nestedFiles() throws URISyntaxException {
        Path texFile = Path.of(DefaultTexParserTest.class.getResource("nested.tex").toURI());

        LatexParserResult parserResult = LATEX_PARSER.parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult(texFile);

        expectedParserResult.addBibFile(texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addNestedFile(texFile.getParent().resolve("nested2.tex"));

        assertEquals(expectedParserResult, parserResult);
    }
}
