package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.texparser.TexBibEntriesResolverResult;
import org.jabref.model.texparser.TexParserResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TexBibEntriesResolverTest {
    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_B = "Einstein1920b";
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String EINSTEIN_21 = "Einstein1921";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";

    private static BibDatabase database;

    @BeforeEach
    private void setUp() {
        database = new BibDatabase();

        BibEntry darwin = new BibEntry(StandardEntryType.Book)
                .withCiteKey(DARWIN)
                .withField(StandardField.TITLE, "The descent of man, and selection in relation to sex")
                .withField(StandardField.PUBLISHER, "J. Murray")
                .withField(StandardField.YEAR, "1888")
                .withField(StandardField.AUTHOR, "Darwin, Charles");
        database.insertEntry(darwin);

        BibEntry einstein = new BibEntry(StandardEntryType.Book)
                .withCiteKey(EINSTEIN)
                .withField(StandardField.TITLE, "Relativity: The special and general theory")
                .withField(StandardField.PUBLISHER, "Penguin")
                .withField(StandardField.YEAR, "1920")
                .withField(StandardField.AUTHOR, "Einstein, Albert");
        database.insertEntry(einstein);

        BibEntry newton = new BibEntry(StandardEntryType.Book)
                .withCiteKey(NEWTON)
                .withField(StandardField.TITLE, "The Principia: mathematical principles of natural philosophy")
                .withField(StandardField.PUBLISHER, "Univ of California Press")
                .withField(StandardField.YEAR, "1999")
                .withField(StandardField.AUTHOR, "Newton, Isaac");
        database.insertEntry(newton);

        BibEntry einsteinA = new BibEntry(StandardEntryType.Book)
                .withCiteKey(EINSTEIN_A)
                .withField(StandardField.CROSSREF, "Einstein1920")
                .withField(StandardField.PAGES, "22--23");
        database.insertEntry(einsteinA);

        BibEntry einsteinB = new BibEntry(StandardEntryType.Book)
                .withCiteKey(EINSTEIN_B)
                .withField(StandardField.CROSSREF, "Einstein1921")
                .withField(StandardField.PAGES, "22--23");
        database.insertEntry(einsteinB);

        BibEntry einsteinC = new BibEntry(StandardEntryType.Book)
                .withCiteKey(EINSTEIN_C)
                .withField(StandardField.CROSSREF, "Einstein1920")
                .withField(StandardField.PAGES, "25--33");
        database.insertEntry(einsteinC);
    }

    @Test
    public void testSingleFile() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(texFile);

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testTwoFiles() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Paths.get(TexBibEntriesResolverTest.class.getResource("paper2.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile2));

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);
        expectedCrossingResult.insertEntry(database, NEWTON);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testDuplicateFiles() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("paper.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(texFile);

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testUnknownKey() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("unknown_key.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(texFile);

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);
        expectedCrossingResult.addUnresolvedKey(UNKNOWN);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testNestedFiles() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("nested.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(texFile);

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);

        assertEquals(expectedCrossingResult, crossingResult);
    }

    @Test
    public void testCrossRef() throws URISyntaxException {
        Path texFile = Paths.get(TexBibEntriesResolverTest.class.getResource("crossref.tex").toURI());
        TexParserResult parserResult = new DefaultTexParser().parse(texFile);

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(parserResult);

        expectedCrossingResult.insertEntry(database, EINSTEIN_B);
        expectedCrossingResult.insertEntry(database, EINSTEIN_A);
        expectedCrossingResult.insertEntry(database, EINSTEIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN_C);
        expectedCrossingResult.addUnresolvedKey(EINSTEIN_21);
        expectedCrossingResult.addUnresolvedKey(UNRESOLVED);
        expectedCrossingResult.increaseCrossRefsCount();

        assertEquals(expectedCrossingResult, crossingResult);
    }
}
