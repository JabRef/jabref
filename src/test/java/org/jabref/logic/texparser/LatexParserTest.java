package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LatexParserTest {
    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_B = "Einstein1920b";
    private final static String EINSTEIN_C = "Einstein1920c";

    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();
    private GeneralPreferences generalPreferences;
    private ImportFormatPreferences importFormatPreferences;
    private BibDatabase database;
    private BibDatabase database2;

    @BeforeEach
    private void setUp() {
        generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        database = new BibDatabase();
        database2 = new BibDatabase();

        BibEntry darwin = new BibEntry(StandardEntryType.Book)
                .withCitationKey(DARWIN)
                .withField(StandardField.TITLE, "The descent of man, and selection in relation to sex")
                .withField(StandardField.PUBLISHER, "J. Murray")
                .withField(StandardField.YEAR, "1888")
                .withField(StandardField.AUTHOR, "Darwin, Charles");
        database.insertEntry(darwin);

        BibEntry einstein = new BibEntry(StandardEntryType.Book)
                .withCitationKey(EINSTEIN)
                .withField(StandardField.TITLE, "Relativity: The special and general theory")
                .withField(StandardField.PUBLISHER, "Penguin")
                .withField(StandardField.YEAR, "1920")
                .withField(StandardField.AUTHOR, "Einstein, Albert");
        database.insertEntry(einstein);

        BibEntry newton = new BibEntry(StandardEntryType.Book)
                .withCitationKey(NEWTON)
                .withField(StandardField.TITLE, "The Principia: mathematical principles of natural philosophy")
                .withField(StandardField.PUBLISHER, "Univ of California Press")
                .withField(StandardField.YEAR, "1999")
                .withField(StandardField.AUTHOR, "Newton, Isaac");
        database.insertEntry(newton);
        database2.insertEntry(newton);

        BibEntry einsteinA = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_A)
                .withField(StandardField.CROSSREF, "Einstein1920")
                .withField(StandardField.PAGES, "22--23");
        database.insertEntry(einsteinA);

        BibEntry einsteinB = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_B)
                .withField(StandardField.CROSSREF, "Einstein1921")
                .withField(StandardField.PAGES, "22--23");
        database.insertEntry(einsteinB);

        BibEntry einsteinC = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_C)
                .withField(StandardField.CROSSREF, "Einstein1920")
                .withField(StandardField.PAGES, "25--33");
        database.insertEntry(einsteinC);
    }

    @Test
    public void testSameFileDifferentDatabases() throws URISyntaxException {
        Path texFile = Path.of(LatexParserTest.class.getResource("paper.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);
        LatexParserResult expectedParserResult = new LatexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addBibFile(texFile, texFile.getParent().resolve("origin.bib"));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(expectedParserResult);

        assertEquals(expectedCrossingResult, crossingResult);

        LatexBibEntriesResolverResult crossingResult2 = new TexBibEntriesResolver(database2, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult2 = new LatexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult2.addEntry(database.getEntryByCitationKey(EINSTEIN).get());
        expectedCrossingResult2.addEntry(database.getEntryByCitationKey(DARWIN).get());

        assertEquals(expectedCrossingResult2, crossingResult2);
    }

    @Test
    public void testTwoFilesDifferentDatabases() throws URISyntaxException {
        Path texFile = Path.of(LatexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Path.of(LatexParserTest.class.getResource("paper2.tex").toURI());

        LatexParserResult parserResult = new DefaultLatexParser().parse(Arrays.asList(texFile, texFile2));
        LatexParserResult expectedParserResult = new LatexParserResult();

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

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(expectedParserResult);

        assertEquals(expectedCrossingResult, crossingResult);

        LatexBibEntriesResolverResult crossingResult2 = new TexBibEntriesResolver(database2, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult2 = new LatexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult2.addEntry(database.getEntryByCitationKey(EINSTEIN).get());
        expectedCrossingResult2.addEntry(database.getEntryByCitationKey(DARWIN).get());

        assertEquals(expectedCrossingResult2, crossingResult2);
    }
}
