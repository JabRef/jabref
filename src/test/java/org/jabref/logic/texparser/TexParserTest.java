package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
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

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        importFormatPreferences = null;
    }

    @Test
    void testSingleFile() throws URISyntaxException, IOException {
        final String DARWIN = "Darwin1888";
        final String EINSTEIN = "Einstein1920";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences,
                    new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());

            // Check entries
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(2, texResult.getCitationsCountByKey(DARWIN));
            assertEquals(2, texResult.getCitationsCountByKey(EINSTEIN));
            assertEquals(2, texResult.getFoundKeysInTex());
            assertEquals(0, texResult.getUnresolvedKeysCount());
            assertEquals(2, texResult.getResolvedKeysCount());
            assertEquals(2, newDatabase.getEntries().size());

            // Check paths
            assertEquals(texFile, texResult.getUniqueKeys().get(DARWIN).get(0).getPath());
            assertEquals(texFile, texResult.getUniqueKeys().get(EINSTEIN).get(0).getPath());

            // Check lines
            assertEquals(4, texResult.getUniqueKeys().get(EINSTEIN).get(0).getLine());
            assertEquals(5, texResult.getUniqueKeys().get(DARWIN).get(0).getLine());
            assertEquals(6, texResult.getUniqueKeys().get(EINSTEIN).get(1).getLine());
            assertEquals(7, texResult.getUniqueKeys().get(DARWIN).get(1).getLine());

            // Check columns
            assertEquals(0, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColStart());
            assertEquals(19, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColEnd());

            assertEquals(67, texResult.getUniqueKeys().get(DARWIN).get(0).getColStart());
            assertEquals(84, texResult.getUniqueKeys().get(DARWIN).get(0).getColEnd());

            assertEquals(14, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColStart());
            assertEquals(33, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColEnd());

            assertEquals(0, texResult.getUniqueKeys().get(DARWIN).get(1).getColStart());
            assertEquals(17, texResult.getUniqueKeys().get(DARWIN).get(1).getColEnd());

            // Check line texts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getLineText());
            assertEquals("Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getLineText());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit. Integer eros tortor, dictum nec aliquet in, pharetra nec justo.",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getLineText());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getLineText());

            // Check contexts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getContext());
            assertEquals("cus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getContext());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsu",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getContext());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getContext());
        }
    }

    @Test
    void testTwoFiles() throws URISyntaxException, IOException {
        final String DARWIN = "Darwin1888";
        final String EINSTEIN = "Einstein1920";
        final String EINSTEIN_A = "Einstein1920a";
        final String EINSTEIN_B = "Einstein1920b";
        final String EINSTEIN_C = "Einstein1920c";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Paths.get(TexParserTest.class.getResource("paper2.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences,
                    new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(Arrays.asList(texFile, texFile2));
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());

            // Check entries
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(2, texResult.getCitationsCountByKey(DARWIN));
            assertEquals(2, texResult.getCitationsCountByKey(EINSTEIN));
            assertEquals(5, texResult.getFoundKeysInTex());
            assertEquals(0, texResult.getUnresolvedKeysCount());
            assertEquals(5, texResult.getResolvedKeysCount());
            assertEquals(5, newDatabase.getEntries().size());

            // Check paths
            assertEquals(texFile, texResult.getUniqueKeys().get(DARWIN).get(0).getPath());
            assertEquals(texFile, texResult.getUniqueKeys().get(EINSTEIN).get(0).getPath());

            assertEquals(texFile2, texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getPath());
            assertEquals(texFile2, texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getPath());
            assertEquals(texFile2, texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getPath());

            // Check lines
            assertEquals(4, texResult.getUniqueKeys().get(EINSTEIN).get(0).getLine());
            assertEquals(5, texResult.getUniqueKeys().get(DARWIN).get(0).getLine());
            assertEquals(6, texResult.getUniqueKeys().get(EINSTEIN).get(1).getLine());
            assertEquals(7, texResult.getUniqueKeys().get(DARWIN).get(1).getLine());

            assertEquals(4, texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getLine());
            assertEquals(5, texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getLine());
            assertEquals(6, texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getLine());

            // Check columns
            assertEquals(0, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColStart());
            assertEquals(19, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColEnd());

            assertEquals(67, texResult.getUniqueKeys().get(DARWIN).get(0).getColStart());
            assertEquals(84, texResult.getUniqueKeys().get(DARWIN).get(0).getColEnd());

            assertEquals(14, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColStart());
            assertEquals(33, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColEnd());

            assertEquals(0, texResult.getUniqueKeys().get(DARWIN).get(1).getColStart());
            assertEquals(17, texResult.getUniqueKeys().get(DARWIN).get(1).getColEnd());

            assertEquals(48, texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getColStart());
            assertEquals(68, texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getColEnd());

            assertEquals(48, texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getColStart());
            assertEquals(68, texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getColEnd());

            assertEquals(48, texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getColStart());
            assertEquals(68, texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getColEnd());

            // Check line texts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getLineText());
            assertEquals("Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getLineText());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit. Integer eros tortor, dictum nec aliquet in, pharetra nec justo.",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getLineText());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getLineText());

            assertEquals("This is some content trying to cite a bib file: \\cite{Einstein1920a}",
                    texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getLineText());
            assertEquals("This is some content trying to cite a bib file: \\cite{Einstein1920b}",
                    texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getLineText());
            assertEquals("This is some content trying to cite a bib file: \\cite{Einstein1920c}",
                    texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getLineText());

            // Check contexts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getContext());
            assertEquals("cus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getContext());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsu",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getContext());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getContext());

            assertEquals("nt trying to cite a bib file: \\cite{Einstein1920a}",
                    texResult.getUniqueKeys().get(EINSTEIN_A).get(0).getContext());
            assertEquals("nt trying to cite a bib file: \\cite{Einstein1920b}",
                    texResult.getUniqueKeys().get(EINSTEIN_B).get(0).getContext());
            assertEquals("nt trying to cite a bib file: \\cite{Einstein1920c}",
                    texResult.getUniqueKeys().get(EINSTEIN_C).get(0).getContext());
        }
    }

    @Test
    void testDuplicateFiles() throws URISyntaxException, IOException {
        final String DARWIN = "Darwin1888";
        final String EINSTEIN = "Einstein1920";

        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences,
                    new DummyFileUpdateMonitor()).parse(originalReader);
            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(Arrays.asList(texFile, texFile));
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertEquals(result.getDatabase(), texResult.getMasterDatabase());

            // Check entries
            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(2, texResult.getCitationsCountByKey(DARWIN));
            assertEquals(2, texResult.getCitationsCountByKey(EINSTEIN));
            assertEquals(2, texResult.getFoundKeysInTex());
            assertEquals(0, texResult.getUnresolvedKeysCount());
            assertEquals(2, texResult.getResolvedKeysCount());
            assertEquals(2, newDatabase.getEntries().size());

            // Check paths
            assertEquals(texFile, texResult.getUniqueKeys().get(DARWIN).get(0).getPath());
            assertEquals(texFile, texResult.getUniqueKeys().get(EINSTEIN).get(0).getPath());

            // Check lines
            assertEquals(4, texResult.getUniqueKeys().get(EINSTEIN).get(0).getLine());
            assertEquals(5, texResult.getUniqueKeys().get(DARWIN).get(0).getLine());
            assertEquals(6, texResult.getUniqueKeys().get(EINSTEIN).get(1).getLine());
            assertEquals(7, texResult.getUniqueKeys().get(DARWIN).get(1).getLine());

            // Check columns
            assertEquals(0, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColStart());
            assertEquals(19, texResult.getUniqueKeys().get(EINSTEIN).get(0).getColEnd());

            assertEquals(67, texResult.getUniqueKeys().get(DARWIN).get(0).getColStart());
            assertEquals(84, texResult.getUniqueKeys().get(DARWIN).get(0).getColEnd());

            assertEquals(14, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColStart());
            assertEquals(33, texResult.getUniqueKeys().get(EINSTEIN).get(1).getColEnd());

            assertEquals(0, texResult.getUniqueKeys().get(DARWIN).get(1).getColStart());
            assertEquals(17, texResult.getUniqueKeys().get(DARWIN).get(1).getColEnd());

            // Check line texts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getLineText());
            assertEquals("Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getLineText());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit. Integer eros tortor, dictum nec aliquet in, pharetra nec justo.",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getLineText());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getLineText());

            // Check contexts
            assertEquals("\\cite{Einstein1920}", texResult.getUniqueKeys().get(EINSTEIN).get(0).getContext());
            assertEquals("cus, eu vehicula enim efficitur.~\\cite{Darwin1888}",
                    texResult.getUniqueKeys().get(DARWIN).get(0).getContext());
            assertEquals("Einstein said~\\cite{Einstein1920} that lorem impsu",
                    texResult.getUniqueKeys().get(EINSTEIN).get(1).getContext());
            assertEquals("\\cite{Darwin1888}.", texResult.getUniqueKeys().get(DARWIN).get(1).getContext());
        }
    }

    @Test
    void testUnknownKey() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("origin.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("unknown_key.tex").toURI());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences,
                    new DummyFileUpdateMonitor()).parse(originalReader);

            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertTrue(texResult.getGeneratedBibDatabase().hasEntries());
            assertEquals(3, texResult.getFoundKeysInTex());
            assertEquals(1, texResult.getUnresolvedKeysCount());
            assertEquals(2, texResult.getResolvedKeysCount());
            assertEquals(2, newDatabase.getEntries().size());
        }
    }

    @Test
    void testFileNotFound() {
        TexParser texParser = new DefaultTexParser(new BibDatabase());
        TexParserResult texResult = texParser.parse(Paths.get("file_not_found.tex"));
        BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

        assertFalse(texResult.getGeneratedBibDatabase().hasEntries());
        assertEquals(0, texResult.getFoundKeysInTex());
        assertEquals(0, texResult.getUnresolvedKeysCount());
        assertEquals(0, texResult.getResolvedKeysCount());
        assertEquals(0, newDatabase.getEntries().size());
    }

    @Test
    void testDuplicateBibDatabaseConfiguration() throws URISyntaxException, IOException {
        InputStream originalStream = TexParserTest.class.getResourceAsStream("config.bib");
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences,
                    new DummyFileUpdateMonitor()).parse(originalReader);

            TexParser texParser = new DefaultTexParser(result.getDatabase());
            TexParserResult texResult = texParser.parse(texFile);
            BibDatabase newDatabase = texResult.getGeneratedBibDatabase();

            assertEquals(Optional.of("\"Maintained by \" # maintainer"), newDatabase.getPreamble());
            assertEquals(1, newDatabase.getStringCount());
        }
    }
}
