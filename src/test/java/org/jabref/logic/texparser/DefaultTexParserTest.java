package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.jabref.model.texparser.TexParserResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultTexParserTest {
    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_B = "Einstein1920b";
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String EINSTEIN_21 = "Einstein1921";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";

    private void testMatchCite(String key, String citeString) {
        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.addKey(key, Paths.get("foo/bar"), 1, 0, citeString.length(), citeString);

        assertEquals(expectedParserResult, texParserResult);
    }

    private void testNonMatchCite(String citeString) {
        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedParserResult = new TexParserResult();

        assertEquals(expectedParserResult, texParserResult);
    }

    @Test
    public void testCiteCommands() {
        testMatchCite(UNRESOLVED, "\\cite[pre][post]{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\cite*{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\parencite[post]{UnresolvedKey}");
        testMatchCite(UNRESOLVED, "\\cite[pre][post]{UnresolvedKey}");
        testMatchCite(EINSTEIN_C, "\\citep{Einstein1920c}");

        testNonMatchCite("\\citet21312{123U123n123resolvedKey}");
        testNonMatchCite("\\1cite[pr234e][post]{UnresolvedKey}");
        testNonMatchCite("\\citep55{5}UnresolvedKey}");
        testNonMatchCite("\\cit2et{UnresolvedKey}");
    }

    @Test
    public void testTwoCitationsSameLine() {
        String citeString = "\\citep{Einstein1920c} and \\citep{Einstein1920a}";

        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.addKey(EINSTEIN_C, Paths.get("foo/bar"), 1, 0, 21, citeString);
        expectedParserResult.addKey(EINSTEIN_A, Paths.get("foo/bar"), 1, 26, 47, citeString);

        assertEquals(expectedParserResult, texParserResult);
    }

    @Test
    public void testSingleFile() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testTwoFiles() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Paths.get(DefaultTexParserTest.class.getResource("paper2.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile2));
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile2));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");
        expectedParserResult.addKey(DARWIN, texFile2, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile2, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(NEWTON, texFile2, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{Newton1999}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testDuplicateFiles() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile));
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testUnknownKey() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("unknown_key.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addKey(DARWIN, texFile, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(UNKNOWN, texFile, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{UnknownKey}");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testFileNotFound() {
        Path texFile = Paths.get("file_not_found.tex");

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testNestedFiles() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("nested.tex").toURI());
        Path texFile2 = Paths.get(DefaultTexParserTest.class.getResource("nested2.tex").toURI());
        Path texFile3 = Paths.get(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.getNestedFiles().addAll(Arrays.asList(texFile2, texFile3));
        expectedParserResult.addKey(EINSTEIN, texFile3, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile3, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile3, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile3, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);
    }
}
