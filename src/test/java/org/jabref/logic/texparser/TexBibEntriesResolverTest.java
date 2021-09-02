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

public class TexBibEntriesResolverTest {
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
    private BibEntry bibEntry;

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

        BibEntry einsteinA = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_A)
                .withField(StandardField.CROSSREF, EINSTEIN)
                .withField(StandardField.PAGES, "22--23");
        database2.insertEntry(einsteinA);

        BibEntry einsteinB = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_B)
                .withField(StandardField.CROSSREF, "Einstein1921")
                .withField(StandardField.PAGES, "22--23");
        database.insertEntry(einsteinB);

        BibEntry einsteinC = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_C)
                .withField(StandardField.CROSSREF, EINSTEIN)
                .withField(StandardField.PAGES, "25--33");
        database.insertEntry(einsteinC);

        bibEntry = new BibEntry(StandardEntryType.InBook)
                .withCitationKey(EINSTEIN_A)
                .withField(StandardField.TITLE, "Relativity: The special and general theory")
                .withField(StandardField.PUBLISHER, "Penguin")
                .withField(StandardField.YEAR, "1920")
                .withField(StandardField.AUTHOR, "Einstein, Albert")
                .withField(StandardField.CROSSREF, "Einstein1920")
                .withField(StandardField.PAGES, "22--23");
    }

    @Test
    public void testSingleFile() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testTwoFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Path.of(TexBibEntriesResolverTest.class.getResource("paper2.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(Arrays.asList(texFile, texFile2));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testDuplicateFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testUnknownKey() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("unknown_key.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testNestedFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("nested.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testCrossRef() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("crossref.tex").toURI());
        LatexParserResult parserResult = new DefaultLatexParser().parse(texFile);

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, generalPreferences, importFormatPreferences, fileMonitor).resolve(parserResult);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.addEntry(bibEntry);

        assertEquals(expectedCrossingResult, crossingResult);
    }
}
