package org.jabref.logic.bibtexkeypattern;

import java.util.Optional;

import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MakeLabelWithDatabaseTest {

    private BibDatabase database;
    private BibtexKeyPatternPreferences preferences;
    private GlobalBibtexKeyPattern pattern;
    private DatabaseBibtexKeyPattern bibtexKeyPattern;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();

        entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "John Doe");
        entry.setField(StandardField.YEAR, "2016");
        entry.setField(StandardField.TITLE, "An awesome paper on JabRef");
        database.insertEntry(entry);
        pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern, ',');
    }

    @Test
    void generateDefaultKey() {
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016a"), entry2.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyAlwaysLetter() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016a"), entry.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');

        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyStartDuplicatesAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');

        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyAlreadyExistsManyDuplicates() {
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        entry2.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "John Doe");
        entry3.setField(StandardField.YEAR, "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        keyGenerator.generateAndSetKey(entry3);
        assertEquals(Optional.of("Doe2016a"), entry3.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyFirstTwoAlreadyExists() {
        BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(bibtexKeyPattern, database, preferences);
        keyGenerator.generateAndSetKey(entry);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "John Doe");
        entry2.setField(StandardField.YEAR, "2016");
        keyGenerator.generateAndSetKey(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "John Doe");
        entry3.setField(StandardField.YEAR, "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        keyGenerator.generateAndSetKey(entry3);
        assertEquals(Optional.of("Doe2016b"), entry3.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthLowerModified() {
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthUpperModified() {
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DOE2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:title_case][year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:sentence_case][year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[auth:capitalize][year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateDefaultKeyFixedValue() {
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DoeTest2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShortYear() {
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("16"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthN() {
        bibtexKeyPattern.setDefaultValue("[auth2]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Do"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthNShortName() {
        bibtexKeyPattern.setDefaultValue("[auth10]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyEmptyField() {
        entry = new BibEntry();
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyEmptyFieldDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        entry.clearField(StandardField.AUTHOR);
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyEmptyFieldNoColonInDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        entry.clearField(StandardField.AUTHOR);
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("ProblemNoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitle() {
        bibtexKeyPattern.setDefaultValue("[title]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperonJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPoJ"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitle() {
        bibtexKeyPattern.setDefaultValue("[shorttitle]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesomepaperJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:lower]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesomepaperjabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:upper]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AWESOMEPAPERJABREF"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:title_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AwesomePaperJabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:sentence_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesomepaperjabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:capitalize]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AwesomePaperJabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitle() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:lower]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:upper]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AWESOME"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:title_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:sentence_case]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyVeryshorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:capitalize]");
        entry.setField(StandardField.TITLE, "An aweSOme Paper on JabRef");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyShorttitleINI() {
        bibtexKeyPattern.setDefaultValue("[shorttitleINI]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Aap"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyCamel() {
        bibtexKeyPattern.setDefaultValue("[camel]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperOnJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthNM() {
        bibtexKeyPattern.setDefaultValue("[auth4_3]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Wond"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthNMLargeN() {
        bibtexKeyPattern.setDefaultValue("[auth20_3]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Wonder"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthNMLargeM() {
        bibtexKeyPattern.setDefaultValue("[auth2_4]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthNMLargeMReallyReturnsEmptyString() {
        bibtexKeyPattern.setDefaultValue("[auth2_4][year]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("2016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyRegExReplace() {
        preferences = new BibtexKeyPatternPreferences("2", "3", false, true, true, pattern, ',');
        bibtexKeyPattern.setDefaultValue("[auth][year]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Doe3016"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthIni() {
        bibtexKeyPattern.setDefaultValue("[authIni2]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DS"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyAuthIniMany() {
        bibtexKeyPattern.setDefaultValue("[authIni10]");
        entry.setField(StandardField.AUTHOR, "John Doe and Donald Smith and Will Wonder");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("DoeSmiWon"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleRegexe() {
        bibtexKeyPattern.setDefaultValue("[title:regex(\" \",\"-\")]");
        entry.setField(StandardField.TITLE, "Please replace the spaces");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("PleaseReplacetheSpaces"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleTitleCase() {
        bibtexKeyPattern.setDefaultValue("[title:title_case]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperonJabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleCapitalize() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AnAwesomePaperOnJabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleSentenceCase() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Anawesomepaperonjabref"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleTitleCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:title_case:abbr]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPoJ"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleCapitalizeAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize:abbr]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("AAPOJ"), entry.getCiteKeyOptional());
    }

    @Test
    void generateKeyTitleSentenceCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case:abbr]");
        new BibtexKeyGenerator(bibtexKeyPattern, database, preferences).generateAndSetKey(entry);
        assertEquals(Optional.of("Aapoj"), entry.getCiteKeyOptional());
    }
}
