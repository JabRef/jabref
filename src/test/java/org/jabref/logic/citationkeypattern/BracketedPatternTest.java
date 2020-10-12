package org.jabref.logic.citationkeypattern;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void nullBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BibEntry another_bibentry = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertThrows(NullPointerException.class, () -> pattern.expand(another_bibentry, ';', another_database));
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
    void testResolvedFieldAndFormat() {
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
    void testResolvedParentNotInDatabase() {
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
    void testEmptyBrackets() {
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

    @Test
    void expandBracketsWithAuthorStartingWithBrackets() {
        // Issue https://github.com/JabRef/jabref/issues/3920
        BibEntry bibEntry = new BibEntry()
                .withField(StandardField.AUTHOR, "Patrik {\\v{S}}pan{\\v{e}}l and Kseniya Dryahina and David Smith");
        assertEquals("ŠpanělEtAl", BracketedPattern.expandBrackets("[authEtAl:latex_to_unicode]", null, bibEntry, null));
    }

    @Test
    void expandBracketsWithModifierContainingRegexCharacterCkass() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Wickedness:Managing");

        assertEquals("Wickedness.Managing", BracketedPattern.expandBrackets("[title:regex(\"[:]+\",\".\")]", null, bibEntry, null));
    }

    @Test
    void expandBracketsEmptyStringFromEmptyBrackets() {
        BibEntry bibEntry = new BibEntry();

        assertEquals("", BracketedPattern.expandBrackets("[]", null, bibEntry, null));
    }
}
