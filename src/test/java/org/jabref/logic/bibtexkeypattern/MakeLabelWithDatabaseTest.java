package org.jabref.logic.bibtexkeypattern;

import java.util.Optional;

import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MakeLabelWithDatabaseTest {

    private BibDatabase database;
    private BibtexKeyPatternPreferences preferences;
    private GlobalBibtexKeyPattern pattern;
    private DatabaseBibtexKeyPattern bibtexKeyPattern;
    private BibEntry entry;

    @Before
    public void setUp() {
        database = new BibDatabase();

        entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        entry.setField("title", "An awesome paper on JabRef");
        database.insertEntry(entry);
        pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern, ',');
    }

    @Test
    public void generateDefaultKey() {
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016a"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetter() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016a"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');

        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyStartDuplicatesAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');

        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsManyDuplicates() {
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        entry2.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "John Doe");
        entry3.setField("year", "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016a"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFirstTwoAlreadyExists() {
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry2, preferences);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "John Doe");
        entry3.setField("year", "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016b"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthLowerModified() {
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthUpperModified() {
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DOE2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:title_case][year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[auth:sentence_case][year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[auth:capitalize][year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFixedValue() {
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DoeTest2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShortYear() {
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("16"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthN() {
        bibtexKeyPattern.setDefaultValue("[auth2]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Do"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNShortName() {
        bibtexKeyPattern.setDefaultValue("[auth10]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyField() {
        entry = new BibEntry();
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        entry.clearField("author");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldColonInDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        entry.clearField("author");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Problem:NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitle() {
        bibtexKeyPattern.setDefaultValue("[title]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperonJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AAPoJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitle() {
        bibtexKeyPattern.setDefaultValue("[shorttitle]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Anawesomepaper"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:lower]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("anawesomepaper"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:upper]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("ANAWESOMEPAPER"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:title_case]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaper"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:sentence_case]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Anawesomepaper"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[shorttitle:capitalize]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaper"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitle() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitleLowerModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:lower]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitleUpperModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:upper]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AWESOME"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitleTitleCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:title_case]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitleSentenceCaseModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:sentence_case]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitleCapitalizeModified() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle:capitalize]");
        entry.setField("title", "An aweSOme Paper on JabRef");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleINI() {
        bibtexKeyPattern.setDefaultValue("[shorttitleINI]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Aap"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyCamel() {
        bibtexKeyPattern.setDefaultValue("[camel]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperOnJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNM() {
        bibtexKeyPattern.setDefaultValue("[auth4_3]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Wond"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeN() {
        bibtexKeyPattern.setDefaultValue("[auth20_3]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Wonder"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeM() {
        bibtexKeyPattern.setDefaultValue("[auth2_4]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeMReallyReturnsEmptyString() {
        bibtexKeyPattern.setDefaultValue("[auth2_4][year]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyRegExReplace() {
        preferences = new BibtexKeyPatternPreferences("2", "3", false, true, true, pattern, ',');
        bibtexKeyPattern.setDefaultValue("[auth][year]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe3016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIni() {
        bibtexKeyPattern.setDefaultValue("[authIni2]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DS"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIniMany() {
        bibtexKeyPattern.setDefaultValue("[authIni10]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DoeSmiWon"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleRegexe() {
        bibtexKeyPattern.setDefaultValue("[title:regex(\" \",\"-\")]");
        entry.setField("title", "Please replace the spaces");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Please-Replace-the-Spaces"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleTitleCase() {
        bibtexKeyPattern.setDefaultValue("[title:title_case]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperonJabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleCapitalize() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperOnJabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleSentenceCase() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Anawesomepaperonjabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleTitleCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:title_case:abbr]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AAPoJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleCapitalizeAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize:abbr]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AAPOJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleSentenceCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case:abbr]");
        BibtexKeyPatternUtil.makeAndSetLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Aapoj"), entry.getCiteKeyOptional());
    }
}
