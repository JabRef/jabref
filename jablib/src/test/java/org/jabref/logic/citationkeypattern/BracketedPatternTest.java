package org.jabref.logic.citationkeypattern;

import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests based on a BibEntry are contained in {@link CitationKeyGeneratorTest}
 * <p>
 * "Complete" entries are tested at {@link org.jabref.logic.citationkeypattern.MakeLabelWithDatabaseTest}
 */
@Execution(ExecutionMode.CONCURRENT)
class BracketedPatternTest {

    private BibEntry bibentry;
    private BibDatabase database;
    private BibEntry dbentry;

    @BeforeEach
    void setUp() {
        bibentry = new BibEntry().withField(StandardField.AUTHOR, "O. Kitsune")
                                 .withField(StandardField.YEAR, "2017")
                                 .withField(StandardField.PAGES, "213--216");

        dbentry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("HipKro03")
                .withField(StandardField.AUTHOR, "Eric von Hippel and Georg von Krogh")
                .withField(StandardField.TITLE, "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science")
                .withField(StandardField.JOURNAL, "Organization Science")
                .withField(StandardField.YEAR, "2003")
                .withField(StandardField.VOLUME, "14")
                .withField(StandardField.PAGES, "209--223")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.ADDRESS, "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA")
                .withField(StandardField.DOI, "http://dx.doi.org/10.1287/orsc.14.2.209.14992")
                .withField(StandardField.ISSN, "1526-5455")
                .withField(StandardField.PUBLISHER, "INFORMS");

        database = new BibDatabase();
        database.insertEntry(dbentry);
    }

    static Stream<Arguments> allAuthors() {
        return Stream.of(
                Arguments.of("ArtemenkoEtAl", "Alexander Artemenko and others"),
                Arguments.of("AachenEtAl", "Aachen and others"),
                Arguments.of("AachenBerlinEtAl", "Aachen and Berlin and others"),
                Arguments.of("AachenBerlinChemnitzEtAl", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("AachenBerlinChemnitzDüsseldorf", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("AachenBerlinChemnitzDüsseldorfEtAl", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("AachenBerlinChemnitzDüsseldorfEssen", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("AachenBerlinChemnitzDüsseldorfEssenEtAl", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void allAuthors(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.allAuthors(list));
    }

    static Stream<Arguments> authorsAlpha() {
        return Stream.of(
                Arguments.of("A+", "Alexander Artemenko and others"),
                Arguments.of("A+", "Aachen and others"),
                Arguments.of("AB+", "Aachen and Berlin and others"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authorsAlpha(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authorsAlpha(list));
    }

    static Stream<Arguments> authorsAlphaLNI() {
        return Stream.of(
                Arguments.of("Ar", "Alexander Artemenko and others"),
                Arguments.of("Aa", "Aachen and others"),
                Arguments.of("Aa", "Aachen and Berlin and others"),
                Arguments.of("Aa", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("AB", "Aachen and Berlin"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("Aa", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("Aa", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others"),
                Arguments.of("AB", "Abel, K.; Bibel, U."),
                Arguments.of("ABC", "Abraham, N.; Bibel, U.; Corleone, P."),
                Arguments.of("Az", "Azubi, L. et.al."),
                Arguments.of("Ez", "Ezgarani, O."),
                Arguments.of("GI", "GI, Gesellschaft für Informatik e.V."),
                Arguments.of("Gl", "Glück, H. I."),
                Arguments.of("Go", "von Goethe"),
                Arguments.of("Aa", "van der Aalst"),
                Arguments.of("AW", "van der Aalst and Weske"),
                Arguments.of("GI", "{Gesellschaft für Informatik e.V.}"),
                Arguments.of("AF", "{Apache Foundation}"));
    }

    @ParameterizedTest
    @MethodSource
    void authorsAlphaLNI(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authorsAlphaLNI(list));
    }

    /**
     * Tests [authorIni]
     */
    static Stream<Arguments> oneAuthorPlusInitials() {
        return Stream.of(
                Arguments.of("Aalst", "Wil van der Aalst"),
                Arguments.of("AalstL", "Wil van der Aalst and Tammo van Lessen"),
                Arguments.of("Newto", "I. Newton"),
                Arguments.of("NewtoM", "I. Newton and J. Maxwell"),
                Arguments.of("NewtoME", "I. Newton and J. Maxwell and A. Einstein"),
                Arguments.of("NewtoMEB", "I. Newton and J. Maxwell and A. Einstein and N. Bohr"),
                Arguments.of("NewtoMEBU", "I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown"),
                Arguments.of("Aache+", "Aachen and others"),
                Arguments.of("AacheB", "Aachen and Berlin"),
                Arguments.of("AacheB+", "Aachen and Berlin and others"),
                Arguments.of("AacheBC", "Aachen and Berlin and Chemnitz"),
                Arguments.of("AacheBC+", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("AacheBCD", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("AacheBCD+", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("AacheBCDE", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("AacheBCDE+", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void oneAuthorPlusInitials(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.oneAuthorPlusInitials(list));
    }

    static Stream<Arguments> authShort() {
        return Stream.of(
                Arguments.of("Newton", "Isaac Newton"),
                Arguments.of("NM", "Isaac Newton and James Maxwell"),
                Arguments.of("NME", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("NME+", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aachen", "Aachen"),
                Arguments.of("A+", "Aachen and others"),
                Arguments.of("AB", "Aachen and Berlin"),
                Arguments.of("AB+", "Aachen and Berlin and others"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authIni1(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authIniN(list, 1));
    }

    static Stream<Arguments> authIni1() {
        return Stream.of(
                Arguments.of("N", "Isaac Newton"),
                Arguments.of("N", "Isaac Newton and James Maxwell"),
                Arguments.of("N", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("N", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("A", "Aachen"),
                Arguments.of("A", "Aachen and others"),
                Arguments.of("A", "Aachen and Berlin"),
                Arguments.of("A", "Aachen and Berlin and others"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("A", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authIni2(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authIniN(list, 2));
    }

    static Stream<Arguments> authIni2() {
        return Stream.of(
                Arguments.of("Ne", "Isaac Newton"),
                Arguments.of("NM", "Isaac Newton and James Maxwell"),
                Arguments.of("NM", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("NM", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aa", "Aachen"),
                Arguments.of("A+", "Aachen and others"),
                Arguments.of("AB", "Aachen and Berlin"),
                Arguments.of("AB", "Aachen and Berlin and others"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz"),
                Arguments.of("DS", "John Doe and Donald Smith and Will Wonder"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("AB", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authIni3(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authIniN(list, 3));
    }

    static Stream<Arguments> authIni3() {
        return Stream.of(
                Arguments.of("New", "Isaac Newton"),
                Arguments.of("NeM", "Isaac Newton and James Maxwell"),
                Arguments.of("NME", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("NME", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aac", "Aachen"),
                Arguments.of("Aa+", "Aachen and others"),
                Arguments.of("AaB", "Aachen and Berlin"),
                Arguments.of("AB+", "Aachen and Berlin and others"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz"),
                Arguments.of("DSW", "John Doe and Donald Smith and Will Wonder"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("ABC", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authIni4(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authIniN(list, 4));
    }

    static Stream<Arguments> authIni4() {
        return Stream.of(
                Arguments.of("Newt", "Isaac Newton"),
                Arguments.of("NeMa", "Isaac Newton and James Maxwell"),
                Arguments.of("NeME", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("NMEB", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aach", "Aachen"),
                Arguments.of("Aac+", "Aachen and others"),
                Arguments.of("AaBe", "Aachen and Berlin"),
                Arguments.of("AaB+", "Aachen and Berlin and others"),
                Arguments.of("AaBC", "Aachen and Berlin and Chemnitz"),
                Arguments.of("ABC+", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("ABCD", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authEtAlDotDotEal(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authEtal(list, ".", ".etal"));
    }

    static Stream<Arguments> authEtAlDotDotEal() {
        return Stream.of(
                Arguments.of("Newton", "Isaac Newton"),
                Arguments.of("Newton.Maxwell", "Isaac Newton and James Maxwell"),
                Arguments.of("Newton.etal", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("Newton.etal", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aachen", "Aachen"),
                Arguments.of("Aachen.etal", "Aachen and others"),
                Arguments.of("Aachen.Berlin", "Aachen and Berlin"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and others"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("Aachen.etal", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authAuthEa(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authAuthEa(list));
    }

    static Stream<Arguments> authAuthEa() {
        return Stream.of(
                Arguments.of("Newton", "Isaac Newton"),
                Arguments.of("Newton.Maxwell", "Isaac Newton and James Maxwell"),
                Arguments.of("Newton.Maxwell.ea", "Isaac Newton and James Maxwell and Albert Einstein"),
                Arguments.of("Newton.Maxwell.ea", "Isaac Newton and James Maxwell and Albert Einstein and N. Bohr"),
                Arguments.of("Aachen", "Aachen"),
                Arguments.of("Aachen.ea", "Aachen and others"),
                Arguments.of("Aachen.Berlin", "Aachen and Berlin"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and others"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz and others"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz and Düsseldorf"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz and Düsseldorf and others"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen"),
                Arguments.of("Aachen.Berlin.ea", "Aachen and Berlin and Chemnitz and Düsseldorf and Essen and others")
        );
    }

    @ParameterizedTest
    @MethodSource
    void authShort(String expected, AuthorList list) {
        assertEquals(expected, BracketedPattern.authShort(list));
    }

    @ParameterizedTest
    @CsvSource({
            "'Newton', '[auth]', 'Isaac Newton'",
            "'Newton', '[authFirstFull]', 'Isaac Newton'",
            "'I', '[authForeIni]', 'Isaac Newton'",
            "'Newton', '[auth.etal]', 'Isaac Newton'",
            "'Newton', '[authEtAl]', 'Isaac Newton'",
            "'Newton', '[auth.auth.ea]', 'Isaac Newton'",
            "'Newton', '[authors]', 'Isaac Newton'",
            "'Newton', '[authors2]', 'Isaac Newton'",
            "'Ne', '[authIni2]', 'Isaac Newton'",
            "'New', '[auth3]', 'Isaac Newton'",
            "'New', '[auth3_1]', 'Isaac Newton'",
            "'Newton', '[authshort]', 'Isaac Newton'",
            "'New', '[authorsAlpha]', 'Isaac Newton'",
            "'Ne', '[authorsAlphaLNI]', 'Isaac Newton'",
            "'Newton', '[authorLast]', 'Isaac Newton'",
            "'I', '[authorLastForeIni]', 'Isaac Newton'",

            "'Agency', '[authors]', 'European Union Aviation Safety Agency'",
            "'EUASA', '[authors]', '{European Union Aviation Safety Agency}'"
    })
    void authorFieldMarkers(String expectedCitationKey, String pattern, String author) {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, author);
        BracketedPattern bracketedPattern = new BracketedPattern(pattern);
        assertEquals(expectedCitationKey, bracketedPattern.expand(bibEntry));
    }

    private static Stream<Arguments> expandBracketsWithFallback() {
        return Stream.of(
                Arguments.of("auth", "[title:(auth)]"),
                Arguments.of("auth2021", "[title:(auth[YEAR])]"),
                Arguments.of("not2021", "[title:(not[YEAR])]"),
                Arguments.of("", "[title:([YEAR)]"),
                Arguments.of(")]", "[title:(YEAR])]"),
                Arguments.of("2105.02891", "[title:([EPRINT:([YEAR])])]"),
                Arguments.of("2021", "[title:([auth:([YEAR])])]")
        );
    }

    @ParameterizedTest
    @MethodSource()
    void expandBracketsWithFallback(String expandResult, String pattern) {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.YEAR, "2021").withField(StandardField.EPRINT, "2105.02891");
        BracketedPattern bracketedPattern = new BracketedPattern(pattern);

        assertEquals(expandResult, bracketedPattern.expand(bibEntry));
    }

    @Test
    void expandBracketsWithMissingAuthorAndYear() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "").withField(StandardField.YEAR, "");

        assertEquals(" - ",
                BracketedPattern.expandBrackets("[author] - [year]", ';', bibEntry, database));
    }

    @Test
    void bibentryExpansionTest() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    void nullDatabaseExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    void pureauthReturnsAuthorIfEditorIsAbsent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        assertEquals("Kitsune", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsAuthorIfEditorIsPresent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.setField(StandardField.EDITOR, "Editorlastname, Editorfirstname");
        assertEquals("Kitsune", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsEmptyStringIfAuthorIsAbsent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.clearField(StandardField.AUTHOR);
        assertEquals("", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsEmptyStringIfAuthorIsAbsentAndEditorIsPresent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.clearField(StandardField.AUTHOR);
        bibentry.setField(StandardField.EDITOR, "Editorlastname, Editorfirstname");
        assertEquals("", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void emptyDatabaseExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    void databaseWithStringsExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BibtexString string = new BibtexString("sgr", "Saulius Gražulis");
        another_database.addString(string);
        bibentry = new BibEntry()
                .withField(StandardField.AUTHOR, "#sgr#")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PAGES, "213--216");
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Gražulis_213", pattern.expand(bibentry,
                another_database));
    }

    @Test
    void unbalancedBracketsExpandToSomething() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth_[firstpage]");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    void unbalancedLastBracketExpandsToSomething() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    void entryTypeExpansionTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype]:[year]_[auth]_[pages]");
        assertEquals("Misc:2017_Kitsune_213--216", pattern.expand(bibentry));
    }

    @Test
    void entryTypeExpansionLowercaseTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype:lower]:[year]_[auth]_[firstpage]");
        assertEquals("misc:2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    void suppliedBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        BibEntry another_bibentry = new BibEntry()
                .withField(StandardField.AUTHOR, "Gražulis, Saulius")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PAGES, "213--216");
        assertEquals("2017_Gražulis_213", pattern.expand(another_bibentry, ';', another_database));
    }

    @Test
    void bracketedExpressionDefaultConstructorTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern();
        assertThrows(NullPointerException.class, () -> pattern.expand(bibentry, ';', another_database));
    }

    @Test
    void unknownKeyExpandsToEmptyString() {
        assertEquals("", BracketedPattern.expandBrackets("[unknownkey]", ';', dbentry, database));
    }

    @Test
    void emptyPatternAndEmptyModifierExpandsToEmptyString() {
        assertEquals("", BracketedPattern.expandBrackets("[:]", ';', dbentry, database));
    }

    @Test
    void emptyPatternAndValidModifierExpandsToEmptyString() {
        Character separator = ';';
        assertEquals("", BracketedPattern.expandBrackets("[:lower]", separator, dbentry, database));
    }

    @Test
    void bibtexkeyPatternExpandsToCitationKey() {
        Character separator = ';';
        assertEquals("HipKro03", BracketedPattern.expandBrackets("[bibtexkey]", separator, dbentry, database));
    }

    @Test
    void citationKeyPatternExpandsToCitationKey() {
        Character separator = ';';
        assertEquals("HipKro03", BracketedPattern.expandBrackets("[citationkey]", separator, dbentry, database));
    }

    @Test
    void citationKeyPatternWithEmptyModifierExpandsToBibTeXKey() {
        assertEquals("HipKro03", BracketedPattern.expandBrackets("[citationkey:]", ';', dbentry, database));
    }

    @Test
    void authorPatternTreatsVonNamePrefixCorrectly() {
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", ';', dbentry, database));
    }

    @Test
    void lowerFormatterWorksOnVonNamePrefixes() {
        assertEquals("eric von hippel and georg von krogh",
                BracketedPattern.expandBrackets("[author:lower]", ';', dbentry, database));
    }

    @Test
    void resolvedFieldAndFormat() {
        BibEntry child = new BibEntry().withField(StandardField.CROSSREF, "HipKro03");
        database.insertEntry(child);

        Character separator = ';';
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[unknownkey]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[:]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[:lower]", separator, child, database));

        assertEquals("eric von hippel and georg von krogh",
                BracketedPattern.expandBrackets("[author:lower]", separator, child, database));

        // the citation key is not inherited
        assertEquals("", BracketedPattern.expandBrackets("[citationkey]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[citationkey:]", separator, child, database));
    }

    @Test
    void resolvedParentNotInDatabase() {
        BibEntry child = new BibEntry()
                .withField(StandardField.CROSSREF, "HipKro03");
        database.removeEntry(dbentry);
        database.insertEntry(child);

        assertEquals("", BracketedPattern.expandBrackets("[author]", ';', child, database));
    }

    @Test
    void regularExpressionReplace() {
        assertEquals("2003-JabRef Science",
                BracketedPattern.expandBrackets("[year]-[journal:regex(\"Organization\",\"JabRef\")]", ';', dbentry, database));
    }

    @Test
    void regularExpressionWithBrackets() {
        assertEquals("2003-JabRef Science",
                BracketedPattern.expandBrackets("[year]-[journal:regex(\"[OX]rganization\",\"JabRef\")]", ';', dbentry, database));
    }

    @Test
    void emptyBrackets() {
        assertEquals("2003-Organization Science",
                BracketedPattern.expandBrackets("[year][]-[journal]", ';', dbentry, database));
    }

    /**
     * Test the [:truncate] modifier
     */
    @Test
    void expandBracketsChainsTwoTruncateModifiers() {
        assertEquals("Open",
                BracketedPattern.expandBrackets("[fulltitle:truncate6:truncate5]", ';', dbentry, database));
    }

    @Test
    void expandBracketsDoesNotTruncateWithoutAnArgumentToTruncateModifier() {
        assertEquals("Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science",
                BracketedPattern.expandBrackets("[fulltitle:truncate]", ';', dbentry, database));
    }

    /**
     * Test the [:camel] modifier
     */
    @ParameterizedTest
    @CsvSource({
            "'CamelTitleFormatter', 'Camel Title Formatter'",
            "'CamelTitleFormatter', 'CAMEL TITLE FORMATTER'",
            "'CamelTitleFormatter', 'camel title formatter'",
            "'CamelTitleFormatter', 'cAMEL tITLE fORMATTER'",
            "'C', 'c'"
    })
    void expandBracketsCamelTitleModifier(String expectedCitationKey, String title) {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.TITLE, title);
        assertEquals(expectedCitationKey,
                BracketedPattern.expandBrackets("[title:camel]", ';', bibEntry, null));
    }

    /**
     * Test the [:veryshorttitle] modifier
     */
    @ParameterizedTest
    @CsvSource({
            "'Very', 'A very short title'",
            "'V', 'V'",
            "'V', 'A v'"
    })
    void expandBracketsVeryShortTitleModifier(String expectedCitationKey, String title) {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.TITLE, title);
        assertEquals(expectedCitationKey,
                BracketedPattern.expandBrackets("[title:veryshorttitle]", ';', bibEntry, null));
    }

    /**
     * Test the [:shorttitle] modifier
     */
    @ParameterizedTest
    @CsvSource({
            "'Very Short Title', 'A very short title'",
            "'Short Title', 'Short title'",
            "'Title', 'A title'",
            "'Title', 'A Title'"
    })
    void expandBracketsShortTitleModifier(String expectedCitationKey, String title) {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.TITLE, title);
        assertEquals(expectedCitationKey,
                BracketedPattern.expandBrackets("[title:shorttitle]", ';', bibEntry, null));
    }

    /**
     * Test the [:camelN] modifier
     */
    @Test
    void expandBracketsCamelNModifier() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.TITLE, "Open Source Software And The Private Collective Innovation Model Issues");
        assertEquals("Open",
                BracketedPattern.expandBrackets("[title:camel1]", ';', bibEntry, null));
        assertEquals("OpenSourceSoftwareAnd",
                BracketedPattern.expandBrackets("[title:camel4]", ';', bibEntry, null));
        assertEquals("OpenSourceSoftwareAndThePrivateCollectiveInnovationModelIssues",
                BracketedPattern.expandBrackets("[title:camel10]", ';', bibEntry, null));
    }

    /**
     * Test the [camelN] title marker.
     */
    @Test
    void expandBracketsCamelNTitle() {
        assertEquals("Open",
                BracketedPattern.expandBrackets("[camel1]", ';', dbentry, database));
        assertEquals("OpenSourceSoftwareAnd",
                BracketedPattern.expandBrackets("[camel4]", ';', dbentry, database));
        assertEquals("OpenSourceSoftwareAndThePrivateCollectiveInnovationModelIssues",
                BracketedPattern.expandBrackets("[camel10]", ';', dbentry, database));
    }

    @Test
    void expandBracketsWithAuthorStartingWithBrackets() {
        // Issue https://github.com/JabRef/jabref/issues/3920
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Patrik {\\v{S}}pan{\\v{e}}l and Kseniya Dryahina and David Smith");
        assertEquals("ŠpanělEtAl", BracketedPattern.expandBrackets("[authEtAl:latex_to_unicode]", null, bibEntry, null));
    }

    @Test
    void expandBracketsWithModifierContainingRegexCharacterClass() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Wickedness:Managing");
        assertEquals("Wickedness.Managing", BracketedPattern.expandBrackets("[title:regex(\"[:]+\",\".\")]", null, bibEntry, null));
    }

    @Test
    void regExForFirstWord() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "First Second Third");
        assertEquals("First", BracketedPattern.expandBrackets("[TITLE:regex(\"(\\w+).*\",\"$1\")]", null, bibEntry, null));
    }

    @Test
    void regExWithComma() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "First,Second,Third");
        assertEquals("First+Second+Third", BracketedPattern.expandBrackets("[TITLE:regex(\",\",\"+\")]", null, bibEntry, null));
    }

    @Test
    void regExWithEscapedQuote() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "First\"Second\"Third");
        assertEquals("First+Second+Third", BracketedPattern.expandBrackets("[TITLE:regex(\"\\\"\",\"+\")]", null, bibEntry, null));
    }

    @Test
    void regExWithEtAlTwoAuthors() {
        // Example from https://docs.jabref.org/setup/citationkeypatterns#modifiers
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "First Last and Second Last");
        assertEquals("LastAndLast", BracketedPattern.expandBrackets("[auth.etal:regex(\"\\.etal\",\"EtAl\"):regex(\"\\.\",\"And\")]", null, bibEntry, null));
    }

    @Test
    void regExWithEtAlThreeAuthors() {
        // Example from https://docs.jabref.org/setup/citationkeypatterns#modifiers
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "First Last and Second Last and Third Last");
        assertEquals("LastEtAl", BracketedPattern.expandBrackets("[auth.etal:regex(\"\\.etal\",\"EtAl\"):regex(\"\\.\",\"And\")]", null, bibEntry, null));
    }

    @Test
    void expandBracketsEmptyStringFromEmptyBrackets() {
        BibEntry bibEntry = new BibEntry();
        assertEquals("", BracketedPattern.expandBrackets("[]", null, bibEntry, null));
    }

    @Test
    void expandBracketsInstitutionAbbreviationFromProvidedAbbreviation() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "{European Union Aviation Safety Agency ({EUASABRACKET})}");
        assertEquals("EUASABRACKET", BracketedPattern.expandBrackets("[auth]", null, bibEntry, null));
    }

    @Test
    void expandBracketsInstitutionAbbreviationForAuthorContainingUnion() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "{European Union Aviation Safety Agency}");
        assertEquals("EUASA", BracketedPattern.expandBrackets("[auth]", null, bibEntry, null));
    }

    @Test
    void expandBracketsLastNameForAuthorStartingWithOnlyLastNameStartingWithLowerCase() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "{eBay}");
        assertEquals("eBay", BracketedPattern.expandBrackets("[auth]", null, bibEntry, null));
    }

    @Test
    void expandBracketsLastNameWithChineseCharacters() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "杨秀群");
        assertEquals("杨秀群", BracketedPattern.expandBrackets("[auth]", null, bibEntry, null));
    }

    @Test
    void expandBracketsUnmodifiedStringFromLongFirstPageNumber() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.PAGES, "2325967120921344");
        assertEquals("2325967120921344", BracketedPattern.expandBrackets("[firstpage]", null, bibEntry, null));
    }

    @Test
    void expandBracketsUnmodifiedStringFromLongLastPageNumber() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.PAGES, "2325967120921344");
        assertEquals("2325967120921344", BracketedPattern.expandBrackets("[lastpage]", null, bibEntry, null));
    }

    @Test
    void expandBracketsWithTestCasesFromRegExpBasedFileFinder() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("HipKro03")
                .withField(StandardField.AUTHOR, "Eric von Hippel and Georg von Krogh")
                .withField(StandardField.TITLE, "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science")
                .withField(StandardField.JOURNAL, "Organization Science")
                .withField(StandardField.YEAR, "2003")
                .withField(StandardField.VOLUME, "14")
                .withField(StandardField.PAGES, "209--223")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.ADDRESS, "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA")
                .withField(StandardField.DOI, "http://dx.doi.org/10.1287/orsc.14.2.209.14992")
                .withField(StandardField.ISSN, "1526-5455")
                .withField(StandardField.PUBLISHER, "INFORMS");

        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);

        assertEquals("", BracketedPattern.expandBrackets("", ',', entry, database));

        assertEquals("dropped", BracketedPattern.expandBrackets("drop[unknownkey]ped", ',', entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", ',', entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                BracketedPattern.expandBrackets("[author] are two famous authors.", ',', entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                BracketedPattern.expandBrackets("[author] are two famous authors.", ',', entry, database));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                BracketedPattern.expandBrackets("[author] have published [fulltitle] in [journal].", ',', entry, database));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                BracketedPattern.expandBrackets("[author] have published [title] in [journal].", ',', entry, database));
    }

    @Test
    void expandBracketsWithoutProtectiveBracesUsingUnprotectTermsModifier() {
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "{ACS} Medicinal Chemistry Letters");
        assertEquals("ACS Medicinal Chemistry Letters", BracketedPattern.expandBrackets("[JOURNAL:unprotect_terms]", null, bibEntry, null));
    }

    @ParameterizedTest
    @CsvSource({
            "'Newton', '[edtr]', 'Isaac Newton'",
            "'I', '[edtrForeIni]', 'Isaac Newton'",
            "'Newton', '[editors]', 'Isaac Newton'",
            "'Ne', '[edtrIni2]', 'Isaac Newton'",
            "'New', '[edtr3]', 'Isaac Newton'",
            "'Newton', '[edtr7]', 'Isaac Newton'",
            "'New', '[edtr3_1]', 'Isaac Newton'",
            "'Newton.Maxwell', '[edtr.edtr.ea]', 'Isaac Newton and James Maxwell'",
            "'Newton', '[edtrshort]', 'Isaac Newton'",
            "'Newton', '[editorLast]', 'Isaac Newton'",
            "'I', '[editorLastForeIni]', 'Isaac Newton'",

            "'EUASA', '[editors]', '{European Union Aviation Safety Agency}'"
    })
    void editorFieldMarkers(String expectedCitationKey, String pattern, String editor) {
        BibEntry bibEntry = new BibEntry().withField(StandardField.EDITOR, editor);
        BracketedPattern bracketedPattern = new BracketedPattern(pattern);
        assertEquals(expectedCitationKey, bracketedPattern.expand(bibEntry));
    }

    @ParameterizedTest
    @CsvSource({
            "'', ''",
            "The Attributed Graph Grammar System ({AGG}),AGG",
            "'The University of Science',UniScience",
            "'School of Business, Department of Management',BM",
            "'Graph Systems Research Group',GSRG",
            "'The Great Institute, 123 Main Street, Springfield',GreatInstitute",
            "'Invalid {\\Unicode}',Invalid",
            "'School of Electrical Engineering ({SEE}), Department of Computer Science',SEE",
            "'{The Attributed Graph Grammar System ({AGG})}',AGG",
            "'{The Attributed Graph Grammar System}',AGGS",
            "'{University of Example, Department of Computer Science, Some Address}',UniExampleCS",
            "'{Example School of Engineering, Department of Computer Science, Some Address}',SomeAddressEECS",
            "'{Example Institute, Computer Science Department, Some Address}',ExampleInstituteCS",
            "'{Short Part, Some Address}',ShortPart",
            "'{Example with Several Tokens, Some Address}',EST"})
    void generateInstitutionKeyTest(String input, String expected) {
        assertEquals(expected, BracketedPattern.generateInstitutionKey(input));
    }

    @Test
    void generateInstitutionKeyNullTest() {
        assertNull(BracketedPattern.generateInstitutionKey(null));
    }
}
