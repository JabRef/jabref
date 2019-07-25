package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
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

        BibEntry darwin = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", DARWIN)
                .withField("title", "The descent of man, and selection in relation to sex")
                .withField("publisher", "J. Murray")
                .withField("year", "1888")
                .withField("author", "Darwin, Charles");
        database.insertEntry(darwin);

        BibEntry einstein = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN)
                .withField("title", "Relativity: The special and general theory")
                .withField("publisher", "Penguin")
                .withField("year", "1920")
                .withField("author", "Einstein, Albert");
        database.insertEntry(einstein);

        BibEntry newton = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", NEWTON)
                .withField("title", "The Principia: mathematical principles of natural philosophy")
                .withField("publisher", "Univ of California Press")
                .withField("year", "1999")
                .withField("author", "Newton, Isaac");
        database.insertEntry(newton);

        BibEntry einsteinA = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_A)
                .withField("crossref", "Einstein1920")
                .withField("pages", "22--23");
        database.insertEntry(einsteinA);

        BibEntry einsteinB = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_B)
                .withField("crossref", "Einstein1921")
                .withField("pages", "22--23");
        database.insertEntry(einsteinB);

        BibEntry einsteinC = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_C)
                .withField("crossref", "Einstein1920")
                .withField("pages", "25--33");
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
