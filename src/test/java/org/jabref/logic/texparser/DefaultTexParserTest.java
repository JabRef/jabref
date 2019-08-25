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
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";

    private void testMatchCite(String key, String citeString) {
        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.addKey(key, Paths.get(""), 1, 0, citeString.length(), citeString);

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
    public void testTwoCitationsSameLine() {
        String citeString = "\\citep{Einstein1920c} and \\citep{Einstein1920a}";

        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.addKey(EINSTEIN_C, Paths.get(""), 1, 0, 21, citeString);
        expectedParserResult.addKey(EINSTEIN_A, Paths.get(""), 1, 26, 47, citeString);

        assertEquals(expectedParserResult, texParserResult);
    }

    @Test
    public void testFileEncodingUtf8() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("utf-8.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addKey("anykey", texFile, 1, 32, 45, "Danach wir anschließend mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testFileEncodingIso88591() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("iso-8859-1.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult
                .addKey("anykey", texFile, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testFileEncodingIso885915() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("iso-8859-15.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        // The character � is on purpose - we cannot use Apache Tika's CharsetDetector - see ADR-0005
        expectedParserResult
                .addKey("anykey", texFile, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testFileEncodingForThreeFiles() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("utf-8.tex").toURI());
        Path texFile2 = Paths.get(DefaultTexParserTest.class.getResource("iso-8859-1.tex").toURI());
        Path texFile3 = Paths.get(DefaultTexParserTest.class.getResource("iso-8859-15.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser()
                .parse(Arrays.asList(texFile, texFile2, texFile3));
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile2, texFile3));
        expectedParserResult
                .addKey("anykey", texFile, 1, 32, 45, "Danach wir anschließend mittels \\cite{anykey}.");
        expectedParserResult
                .addKey("anykey", texFile2, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");
        expectedParserResult
                .addKey("anykey", texFile3, 1, 32, 45, "Danach wir anschlie�end mittels \\cite{anykey}.");

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    public void testSingleFile() throws URISyntaxException {
        Path texFile = Paths.get(DefaultTexParserTest.class.getResource("paper.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
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
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addBibFile(texFile2, texFile2.getParent().resolve("origin.bib"));
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
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
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
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
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
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addBibFile(texFile2, texFile2.getParent().resolve("origin.bib"));
        expectedParserResult.addBibFile(texFile3, texFile3.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(EINSTEIN, texFile3, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile3, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile3, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile3, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        assertEquals(expectedParserResult, parserResult);
    }
}
