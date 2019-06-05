package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.CrossingKeysResult;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TexParserTest {
    private final String DARWIN = "Darwin1888";
    private final String EINSTEIN = "Einstein1920";
    private final String UNRESOLVED = "UnresolvedKey";
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        importFormatPreferences = null;
    }

    private void testCite(String key, String citeString, boolean match) {
        TexParserResult texParserResult = new DefaultTexParser().parse(citeString);
        TexParserResult expectedResult = new TexParserResult();

        if (match) {
            expectedResult.getCitations().put(key, new ArrayList<>());
            expectedResult.getCitations().get(key).add(
                    new Citation(Paths.get("foo/bar"), 1, 0, citeString.length(), citeString));
        }

        assertEquals(expectedResult, texParserResult);
    }

    private void testMatchCite(String citeString) {
        testCite(UNRESOLVED, citeString, true);
    }

    private void testNonMatchCite(String citeString) {
        testCite(UNRESOLVED, citeString, false);
    }

    @Test
    void testCiteCommands() {
        testMatchCite("\\cite[pre][post]{UnresolvedKey}");
        testMatchCite("\\cite*{UnresolvedKey}");
        testMatchCite("\\parencite[post]{UnresolvedKey}");
        testMatchCite("\\cite[pre][post]{UnresolvedKey}");
        testMatchCite("\\citep{UnresolvedKey}");

        testNonMatchCite("\\citet21312{123U123n123resolvedKey}");
        testNonMatchCite("\\1cite[pr234e][post]{UnresolvedKey}");
        testNonMatchCite("\\citep55{5}UnresolvedKey}");
        testNonMatchCite("\\cit2et{UnresolvedKey}");
    }

    private void addCite(CrossingKeysResult expectedResult, String key, Path texFile, int line, int colStart, int colEnd, String lineText, boolean insert) {
        Citation citation = new Citation(texFile, line, colStart, colEnd, lineText);

        if (!expectedResult.getParserResult().getCitations().containsKey(key)) {
            expectedResult.getParserResult().getCitations().put(key, new ArrayList<>());
        }

        if (!expectedResult.getParserResult().getCitations().get(key).contains(citation)) {
            expectedResult.getParserResult().getCitations().get(key).add(citation);
        }

        if (insert) {
            BibEntry clonedEntry = (BibEntry) expectedResult.getMasterDatabase().getEntryByKey(key).get().clone();
            expectedResult.getNewDatabase().insertEntry(clonedEntry);
        } else {
            expectedResult.getUnresolvedKeys().add(key);
        }
    }

    @Test
    void testSingleFile() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(texFile);
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().add(texFile);

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase());
            addCite(expectedCrossingResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", true);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }

    @Test
    void testTwoFiles() throws URISyntaxException, IOException {
        String NEWTON = "Newton1999";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Paths.get(TexParserTest.class.getResource("paper2.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile2));
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile2));

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase());
            addCite(expectedCrossingResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", true);
            addCite(expectedCrossingResult, EINSTEIN, texFile2, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", true);
            addCite(expectedCrossingResult, DARWIN, texFile2, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}", true);
            addCite(expectedCrossingResult, NEWTON, texFile2, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{Newton1999}", true);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }

    @Test
    void testDuplicateFiles() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile));
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile));

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase());
            addCite(expectedCrossingResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", true);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }

    @Test
    void testUnknownKey() throws URISyntaxException, IOException {
        String UNKNOWN = "UnknownKey";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("unknown_key.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(texFile);
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().add(texFile);

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase());
            addCite(expectedCrossingResult, EINSTEIN, texFile, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, DARWIN, texFile, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}", true);
            addCite(expectedCrossingResult, UNKNOWN, texFile, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{UnknownKey}", false);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }

    @Test
    void testFileNotFound() {
        TexParserResult parserResult = new DefaultTexParser().parse(Paths.get("file_not_found.tex"));
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(Paths.get("file_not_found.tex"));

        assertEquals(expectedParserResult, parserResult);
    }

    @Test
    void testDuplicateBibDatabaseConfiguration() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("config.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(texFile);
            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();

            assertEquals(Optional.of("\"Maintained by \" # maintainer"), crossingResult.getNewDatabase().getPreamble());
            assertEquals(1, crossingResult.getNewDatabase().getStringCount());
        }
    }

    @Test
    void testNestedFiles() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("nested.tex").toURI());
        Path texFile2 = Paths.get(TexParserTest.class.getResource("nested2.tex").toURI());
        Path texFile3 = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(texFile);
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().add(texFile);
            expectedParserResult.getNestedFiles().addAll(Arrays.asList(texFile2, texFile3));

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase());

            addCite(expectedCrossingResult, EINSTEIN, texFile.getParent().resolve("paper.tex"), 4, 0, 19, "\\cite{Einstein1920}", true);
            addCite(expectedCrossingResult, EINSTEIN, texFile.getParent().resolve("paper.tex"), 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", true);
            addCite(expectedCrossingResult, DARWIN, texFile.getParent().resolve("paper.tex"), 5, 0, 17, "\\cite{Darwin1888}.", true);
            addCite(expectedCrossingResult, DARWIN, texFile.getParent().resolve("paper.tex"), 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", true);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }

    @Test
    void testCrossRef() throws URISyntaxException, IOException {
        String EINSTEIN_A = "Einstein1920a";
        String EINSTEIN_B = "Einstein1920b";
        String EINSTEIN21 = "Einstein1921";
        String EINSTEIN_C = "Einstein1920c";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("crossref.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);

            TexParserResult parserResult = new DefaultTexParser().parse(texFile);
            TexParserResult expectedParserResult = new TexParserResult();
            expectedParserResult.getFileList().add(texFile);

            CrossingKeysResult crossingResult = new CrossingKeys(parserResult, result.getDatabase()).resolveKeys();
            CrossingKeysResult expectedCrossingResult = new CrossingKeysResult(expectedParserResult, result.getDatabase(), 0, 1);

            expectedParserResult.getCitations().put(EINSTEIN_A, new ArrayList<>());
            expectedParserResult.getCitations().get(EINSTEIN_A).add(new Citation(texFile, 4, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920a}"));
            expectedCrossingResult.getNewDatabase().insertEntry((BibEntry) expectedCrossingResult.getMasterDatabase().getEntryByKey(EINSTEIN_A).get().clone());
            expectedCrossingResult.getNewDatabase().insertEntry((BibEntry) expectedCrossingResult.getMasterDatabase().getEntryByKey(EINSTEIN).get().clone());

            expectedParserResult.getCitations().put(EINSTEIN_B, new ArrayList<>());
            expectedParserResult.getCitations().get(EINSTEIN_B).add(new Citation(texFile, 5, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920b}"));
            expectedCrossingResult.getNewDatabase().insertEntry((BibEntry) expectedCrossingResult.getMasterDatabase().getEntryByKey(EINSTEIN_B).get().clone());
            expectedCrossingResult.getUnresolvedKeys().add(EINSTEIN21);

            expectedParserResult.getCitations().put(EINSTEIN_C, new ArrayList<>());
            expectedParserResult.getCitations().get(EINSTEIN_C).add(new Citation(texFile, 6, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920c}"));
            expectedCrossingResult.getNewDatabase().insertEntry((BibEntry) expectedCrossingResult.getMasterDatabase().getEntryByKey(EINSTEIN_C).get().clone());

            expectedParserResult.getCitations().put(UNRESOLVED, new ArrayList<>());
            expectedParserResult.getCitations().get(UNRESOLVED).add(new Citation(texFile, 7, 48, 68, "This is some content trying to cite a bib file: \\cite{UnresolvedKey}"));
            expectedCrossingResult.getUnresolvedKeys().add(UNRESOLVED);

            assertEquals(expectedCrossingResult, crossingResult);
        }
    }
}
