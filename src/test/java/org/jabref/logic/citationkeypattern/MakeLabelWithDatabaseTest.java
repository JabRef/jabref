package org.jabref.logic.citationkeypattern;

import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MakeLabelWithDatabaseTest {

    private BibDatabase database;
    private CitationKeyPatternPreferences preferences;
    private GlobalCitationKeyPattern pattern;
    private DatabaseCitationKeyPattern bibtexKeyPattern;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();

        entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "John Doe");
        entry.setField(StandardField.YEAR, "2016");
        entry.setField(StandardField.TITLE, "An awesome paper on JabRef");
        database.insertEntry(entry);
        pattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
        bibtexKeyPattern = new DatabaseCitationKeyPattern(pattern);
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');
    }

    @Test
    void generateDefaultKey() {
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016a"), entry2.getCitationKey());
    }

    @Test
    void generateDefaultKeyAlwaysLetter() {
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.ALWAYS,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');

        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016a"), entry.getCitationKey());
    }

    @Test
    void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.ALWAYS,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');

        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016b"), entry2.getCitationKey());
    }

    @Test
    void generateDefaultKeyStartDuplicatesAtB() {
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');

        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');

        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016b"), entry2.getCitationKey());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void generateDefaultKeyAlreadyExistsManyDuplicates() {
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        entry2.setCitationKey(entry.getCitationKey().get());
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "John Doe");
        entry3.setField(StandardField.YEAR, "2016");
        entry3.setCitationKey(entry.getCitationKey().get());
        database.insertEntry(entry3);
        keyGenerator.generateAndSetKey(entry3);
        assertEquals(Optional.of("Doe2016a"), entry3.getCitationKey());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void generateDefaultKeyFirstTwoAlreadyExists() {
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "John Doe");
        entry3.setField(StandardField.YEAR, "2016");
        entry3.setCitationKey(entry.getCitationKey().get());
        database.insertEntry(entry3);
        keyGenerator.generateAndSetKey(entry3);
        assertEquals(Optional.of("Doe2016b"), entry3.getCitationKey());
    }

    @Test
    void generateKeyAuthLowerModified() {
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("doe2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthUpperModified() {
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DOE2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:title_case][year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:sentence_case][year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[auth:capitalize][year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCitationKey());
    }

    @Test
    void generateDefaultKeyFixedValue() {
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DoeTest2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyShortYear() {
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("16"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthN() {
        bibtexKeyPattern.setDefaultValue("[auth2]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Do"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthNShortName() {
        bibtexKeyPattern.setDefaultValue("[auth10]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe"), entry.getCitationKey());
    }

    @Test
    void generateKeyEmptyField() {
        entry = new BibEntry();
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.empty(), entry.getCitationKey());
    }

    @Test
    void generateKeyEmptyFieldDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        entry.clearField(StandardField.AUTHOR);
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCitationKey());
    }

    @Test
    void generateKeyEmptyFieldNoColonInDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        entry.clearField(StandardField.AUTHOR);
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("ProblemNoAuthorProvided"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitle() {
        bibtexKeyPattern.setDefaultValue("[title]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperonJabRef"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPoJ"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitle() {
        bibtexKeyPattern.setDefaultValue("[shorttitle]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesomepaperJabRef"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:lower]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesomepaperjabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:upper]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AWESOMEPAPERJABREF"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:title_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AwesomePaperJabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:sentence_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesomepaperjabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:capitalize]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AwesomePaperJabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitle() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesome"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:lower]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesome"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:upper]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AWESOME"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:title_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:sentence_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCitationKey());
    }

    @Test
    void generateKeyVeryshorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:capitalize]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCitationKey());
    }

    @Test
    void generateKeyShorttitleINI() {
        bibtexKeyPattern.setDefaultValue("[shorttitleINI]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Aap"), entry.getCitationKey());
    }

    @Test
    void generateKeyCamel() {
        bibtexKeyPattern.setDefaultValue("[camel]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperOnJabRef"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthNM() {
        bibtexKeyPattern.setDefaultValue("[auth4_3]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Wond"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthNMLargeN() {
        bibtexKeyPattern.setDefaultValue("[auth20_3]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Wonder"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthNMLargeM() {
        bibtexKeyPattern.setDefaultValue("[auth2_4]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.empty(), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthNMLargeMReallyReturnsEmptyString() {
        bibtexKeyPattern.setDefaultValue("[auth2_4][year]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("2016"), entry.getCitationKey());
    }

    @Test
    void generateKeyRegExReplace() {
        preferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "2",
                "3",
                DEFAULT_UNWANTED_CHARACTERS,
                pattern,
                ',');

        bibtexKeyPattern.setDefaultValue("[auth][year]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe3016"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthIni() {
        bibtexKeyPattern.setDefaultValue("[authIni2]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DS"), entry.getCitationKey());
    }

    @Test
    void generateKeyAuthIniMany() {
        bibtexKeyPattern.setDefaultValue("[authIni10]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DoeSmiWon"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleRegexe() {
        bibtexKeyPattern.setDefaultValue("[title:regex(\" \",\"-\")]");
        entry.setField(StandardField.TITLE, "Please replace the spaces");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("PleaseReplacetheSpaces"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleTitleCase() {
        bibtexKeyPattern.setDefaultValue("[title:title_case]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperonJabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleCapitalize() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperOnJabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleSentenceCase() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Anawesomepaperonjabref"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleTitleCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:title_case:abbr]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPoJ"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleCapitalizeAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize:abbr]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPOJ"), entry.getCitationKey());
    }

    @Test
    void generateKeyTitleSentenceCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case:abbr]");
        new CitationKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Aapoj"), entry.getCitationKey());
    }
}
