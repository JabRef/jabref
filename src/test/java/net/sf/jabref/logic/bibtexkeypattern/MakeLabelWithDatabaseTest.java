package net.sf.jabref.logic.bibtexkeypattern;

import java.util.Optional;

import net.sf.jabref.MetaData;
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
    private MetaData metadata;
    private BibtexKeyPatternPreferences preferences;
    private GlobalBibtexKeyPattern pattern;
    private BibEntry entry;

    @Before
    public void setUp() {
        database = new BibDatabase();
        metadata = new MetaData();

        entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        entry.setField("title", "An awesome paper on JabRef");
        database.insertEntry(entry);
        pattern = new GlobalBibtexKeyPattern(AbstractBibtexKeyPattern.split("[auth][year]"));
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
    }

    @Test
    public void generateDefaultKey() {
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016a"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetter() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016a"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern);

        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyStartDuplicatesAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern);

        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsManyDuplicates() {
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
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
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016a"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFirstTwoAlreadyExists() {
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField("author", "John Doe");
        entry3.setField("year", "2016");
        entry3.setCiteKey(entry.getCiteKeyOptional().get());
        database.insertEntry(entry3);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry3, preferences);
        assertEquals(Optional.of("Doe2016b"), entry3.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyLowerModified() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);

        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyUpperModified() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DOE2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFixedValue() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DoeTest2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShortYear() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("16"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthN() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth2]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Do"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNShortName() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth10]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyField() {
        entry = new BibEntry();
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldDefaultText() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.clearField("author");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldColonInDefaultText() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.clearField("author");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Problem:NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitle() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[title]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("AnawesomepaperonJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleAbbr() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("AapoJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitle() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[shorttitle]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("awesomepaperJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyVeryshorttitle() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[veryshorttitle]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("awesome"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShorttitleINI() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[shorttitleINI]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("apJ"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNM() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth4_3]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Wond"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeN() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth20_3]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Wonder"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeM() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth2_4]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNMLargeMReallyReturnsEmptyString() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth2_4][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyRegExReplace() {
        preferences = new BibtexKeyPatternPreferences("2", "3", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe3016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIni() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[authIni2]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DS"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthIniMany() {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[authIni10]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        entry.setField("author", "John Doe and Donald Smith and Will Wonder");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DoeSmiWon"), entry.getCiteKeyOptional());
    }
}
