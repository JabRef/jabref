package net.sf.jabref.logic.bibtexkeypattern;

import java.util.Optional;

import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

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
        pattern = new GlobalBibtexKeyPattern(AbstractBibtexKeyPattern.split("[auth][year]"));
        bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern, ',');
    }

    @Test
    public void generateDefaultKey() {
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016a"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetter() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016a"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern, ',');

        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyStartDuplicatesAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern, ',');

        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsManyDuplicates() {
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
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
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016a"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFirstTwoAlreadyExists() {
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry2, preferences);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "John Doe");
        entry3.setField("year", "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016b"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyLowerModified() {
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyUpperModified() {
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DOE2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFixedValue() {
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DoeTest2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShortYear() {
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("16"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthN() {
        bibtexKeyPattern.setDefaultValue("[auth2]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Do"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNShortName() {
        bibtexKeyPattern.setDefaultValue("[auth10]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyField() {
        entry = new BibEntry();
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        entry.clearField("author");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldColonInDefaultText() {
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        entry.clearField("author");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Problem:NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitle() {
        bibtexKeyPattern.setDefaultValue("[title]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnawesomepaperonJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AapoJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitle() {
        bibtexKeyPattern.setDefaultValue("[shorttitle]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("awesomepaperJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitle() {
        bibtexKeyPattern.setDefaultValue("[veryshorttitle]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleINI() {
        bibtexKeyPattern.setDefaultValue("[shorttitleINI]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("apJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNM() {
        bibtexKeyPattern.setDefaultValue("[auth4_3]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Wond"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeN() {
        bibtexKeyPattern.setDefaultValue("[auth20_3]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Wonder"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeM() {
        bibtexKeyPattern.setDefaultValue("[auth2_4]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeMReallyReturnsEmptyString() {
        bibtexKeyPattern.setDefaultValue("[auth2_4][year]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyRegExReplace() {
        preferences = new BibtexKeyPatternPreferences("2", "3", false, true, true, pattern, ',');
        bibtexKeyPattern.setDefaultValue("[auth][year]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Doe3016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIni() {
        bibtexKeyPattern.setDefaultValue("[authIni2]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DS"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIniMany() {
        bibtexKeyPattern.setDefaultValue("[authIni10]");
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("DoeSmiWon"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleTitleCase() {
        bibtexKeyPattern.setDefaultValue("[title:title_case]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperonJabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleCapitalize() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AnAwesomePaperOnJabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleSentenceCase() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Anawesomepaperonjabref"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleTitleCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:title_case:abbr]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AAPoJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleCapitalizeAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:capitalize:abbr]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("AAPOJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleSentenceCaseAbbr() {
        bibtexKeyPattern.setDefaultValue("[title:sentence_case:abbr]");
        BibtexKeyPatternUtil.makeLabel(bibtexKeyPattern, database, entry, preferences);
        assertEquals(Optional.of("Aapoj"), entry.getCiteKeyOptional());
    }
}
