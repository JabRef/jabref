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
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.TexParser;
import org.jabref.model.texparser.TexParserResult;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TexParserTest {
    private ImportFormatPreferences importFormatPreferences;

    private final String DARWIN = "Darwin1888";
    private final String EINSTEIN = "Einstein1920";
    private final String UNRESOLVED = "UnresolvedKey";

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        importFormatPreferences = null;
    }

    private void addCite(TexParserResult expectedResult, String key, Path texFile, int line, int colStart, int colEnd, String lineText, int resolved) {
        if (!expectedResult.getUniqueKeys().containsKey(key)) {
            expectedResult.getUniqueKeys().put(key, new ArrayList<>());
        } else {
            resolved = 0;
        }

        Citation citation = new Citation(texFile, line, colStart, colEnd, lineText);
        if (!expectedResult.getUniqueKeys().get(key).contains(citation)) {
            expectedResult.getUniqueKeys().get(key).add(citation);
        }

        if (resolved > 0) {
            BibEntry clonedEntry = (BibEntry) expectedResult.getMasterDatabase().getEntryByKey(key).get().clone();
            expectedResult.getGeneratedBibDatabase().insertEntry(clonedEntry);
        } else if (resolved < 0) {
            expectedResult.getUnresolvedKeys().add(key);
        }
    }

    private void testCite(String citeString) throws IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(citeString);
            TexParserResult expectedResult = new TexParserResult(result.getDatabase());
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, UNRESOLVED, Paths.get("foo/bar"), 1, 0, citeString.length(), citeString, -1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertFalse(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
        }
    }

    @Test
    void testCiteCommands() throws IOException {
        testCite("\\cite[pre][post]{UnresolvedKey}");
        testCite("\\cite*{UnresolvedKey}");
        testCite("\\parencite[post]{UnresolvedKey}");
        testCite("\\cite[pre][post]{UnresolvedKey}");
        testCite("\\citep{UnresolvedKey}");
        testCite("\\citet{UnresolvedKey}");
    }

    @Test
    void testSingleFile() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            TexParserResult expectedResult = new TexParserResult(result.getDatabase());
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", 1);
            addCite(expectedResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", 1);

            addCite(expectedResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", 1);
            addCite(expectedResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", 1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
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
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(Arrays.asList(texFile, texFile2));
            TexParserResult expectedResult = new TexParserResult(result.getDatabase());
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", 1);
            addCite(expectedResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", 1);
            addCite(expectedResult, EINSTEIN, texFile2, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}", 1);

            addCite(expectedResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", 1);
            addCite(expectedResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", 1);
            addCite(expectedResult, DARWIN, texFile2, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}", 1);

            addCite(expectedResult, NEWTON, texFile2, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{Newton1999}", 1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
        }
    }

    @Test
    void testDuplicateFiles() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(Arrays.asList(texFile, texFile));
            TexParserResult expectedResult = new TexParserResult(result.getDatabase());
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}", 1);
            addCite(expectedResult, EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", 1);

            addCite(expectedResult, DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.", 1);
            addCite(expectedResult, DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", 1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
        }
    }

    @Test
    void testUnknownKey() throws URISyntaxException, IOException {
        String UNKNOWN = "UnknownKey";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("unknown_key.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            TexParserResult expectedResult = new TexParserResult(result.getDatabase());
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, EINSTEIN, texFile, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}", 1);
            addCite(expectedResult, DARWIN, texFile, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}", 1);
            addCite(expectedResult, UNKNOWN, texFile, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{UnknownKey}", -1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
        }
    }

    @Test
    void testFileNotFound() {
        BibDatabase masterDatabase = new BibDatabase();
        TexParser texParser = new DefaultTexParser(masterDatabase);
        TexParserResult texResult = texParser.parse(Paths.get("file_not_found.tex"));
        TexParserResult expectedResult = new TexParserResult(masterDatabase);
        BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

        assertEquals(masterDatabase, texResult.getMasterDatabase());
        assertFalse(texResult.getGeneratedBibDatabase().hasEntries());
        assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
        assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
        assertEquals(expectedResult, texResult);
    }

    @Test
    void testDuplicateBibDatabaseConfiguration() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("config.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertEquals(Optional.of("\"Maintained by \" # maintainer"), newDatabase.getPreamble());
            assertEquals(1, newDatabase.getStringCount());
        }
    }

    @Test
    void testNestedFiles() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("nested.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            TexParserResult expectedResult = new TexParserResult(result.getDatabase(), 0, 2, 0);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            addCite(expectedResult, EINSTEIN, texFile.getParent().resolve("paper.tex"), 4, 0, 19, "\\cite{Einstein1920}", 1);
            addCite(expectedResult, EINSTEIN, texFile.getParent().resolve("paper.tex"), 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.", 1);

            addCite(expectedResult, DARWIN, texFile.getParent().resolve("paper.tex"), 5, 0, 17, "\\cite{Darwin1888}.", 1);
            addCite(expectedResult, DARWIN, texFile.getParent().resolve("paper.tex"), 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}", 1);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
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
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            TexParserResult expectedResult = new TexParserResult(result.getDatabase(), 0, 0, 1);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            expectedResult.getUniqueKeys().put(EINSTEIN_A, new ArrayList<>());
            expectedResult.getUniqueKeys().get(EINSTEIN_A).add(new Citation(texFile, 4, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920a}"));
            expectedResult.getGeneratedBibDatabase().insertEntry((BibEntry) expectedResult.getMasterDatabase().getEntryByKey(EINSTEIN_A).get().clone());
            expectedResult.getGeneratedBibDatabase().insertEntry((BibEntry) expectedResult.getMasterDatabase().getEntryByKey(EINSTEIN).get().clone());

            expectedResult.getUniqueKeys().put(EINSTEIN_B, new ArrayList<>());
            expectedResult.getUniqueKeys().get(EINSTEIN_B).add(new Citation(texFile, 5, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920b}"));
            expectedResult.getGeneratedBibDatabase().insertEntry((BibEntry) expectedResult.getMasterDatabase().getEntryByKey(EINSTEIN_B).get().clone());
            expectedResult.getUnresolvedKeys().add(EINSTEIN21);

            expectedResult.getUniqueKeys().put(EINSTEIN_C, new ArrayList<>());
            expectedResult.getUniqueKeys().get(EINSTEIN_C).add(new Citation(texFile, 6, 48, 68, "This is some content trying to cite a bib file: \\cite{Einstein1920c}"));
            expectedResult.getGeneratedBibDatabase().insertEntry((BibEntry) expectedResult.getMasterDatabase().getEntryByKey(EINSTEIN_C).get().clone());

            expectedResult.getUniqueKeys().put(UNRESOLVED, new ArrayList<>());
            expectedResult.getUniqueKeys().get(UNRESOLVED).add(new Citation(texFile, 7, 48, 68, "This is some content trying to cite a bib file: \\cite{UnresolvedKey}"));
            expectedResult.getUnresolvedKeys().add(UNRESOLVED);

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(texResult.getFoundKeysInTex() + texResult.getCrossRefEntriesCount(), texResult.getResolvedKeysCount() + texResult.getUnresolvedKeysCount());
            assertEquals(expectedResult.getResolvedKeysCount() + expectedResult.getCrossRefEntriesCount(), newDatabase.getEntries().size());
            assertEquals(expectedResult, texResult);
        }
    }
}
