package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;
import org.jabref.model.texparser.LatexParserResults;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TexBibEntriesResolverTest {
    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_B = "Einstein1920b";
    private final static String EINSTEIN_C = "Einstein1920c";

    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();
    private ImportFormatPreferences importFormatPreferences;
    private BibDatabase database;
    private BibEntry bibEntry;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        database = new BibDatabase();

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
    void singleFile() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        LatexParserResults latexParserResults = new DefaultLatexParser().parse(List.of(texFile));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(latexParserResults);

        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(latexParserResults);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    void twoFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Path.of(TexBibEntriesResolverTest.class.getResource("paper2.tex").toURI());
        LatexParserResults latexParserResults = new DefaultLatexParser().parse(List.of(texFile, texFile2));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(latexParserResults);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(latexParserResults);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    void duplicateFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(parserResults);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResults);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    void unknownKey() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("unknown_key.tex").toURI());
        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(parserResults);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResults);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    void nestedFiles() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("nested.tex").toURI());
        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(parserResults);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResults);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    void crossRef() throws URISyntaxException {
        Path texFile = Path.of(TexBibEntriesResolverTest.class.getResource("crossref.tex").toURI());
        LatexParserResults parserResults = new DefaultLatexParser().parse(List.of(texFile));

        LatexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database, importFormatPreferences, fileMonitor).resolve(parserResults);
        LatexBibEntriesResolverResult expectedCrossingResult = new LatexBibEntriesResolverResult(parserResults);

        expectedCrossingResult.addEntry(bibEntry);

        assertEquals(expectedCrossingResult, crossingResult);
    }
}
