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

    @Before
    public void setUp() {
        database = new BibDatabase();
        metadata = new MetaData();
        pattern = new GlobalBibtexKeyPattern(AbstractBibtexKeyPattern.split("[auth][year]"));
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
    }

    @Test
    public void generateDefaultKey() {
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtA() {
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016a"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetter() {
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016a"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlwaysLetterAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", true, true, true, pattern);

        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyStartDuplicatesAtB() {
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsDuplicatesStartAtB() {
        preferences = new BibtexKeyPatternPreferences("", "", false, false, true, pattern);

        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        database.insertEntry(entry);
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        BibEntry entry2 = new BibEntry();
        entry2.setField("author", "John Doe");
        entry2.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry2, preferences);
        assertEquals(Optional.of("Doe2016b"), entry2.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyAlreadyExistsManyDuplicates() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);

        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        database.insertEntry(entry);
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
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);

        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        database.insertEntry(entry);
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
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);

        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth:lower][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);

        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("doe2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyUpperModified() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth:upper][year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DOE2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateDefaultKeyFixedValue() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth]Test[year]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("DoeTest2016"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyShortYear() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[shortyear]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("16"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthN() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth2]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Do"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyAuthNShortName() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[auth10]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("author", "John Doe");
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Doe"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyField() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        BibEntry entry = new BibEntry();
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.empty(), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldDefaultText() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, true, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[author:(No Author Provided)]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyEmptyFieldColonInDefaultText() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, false, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[author:(Problem:No Author Provided)]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("year", "2016");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("Problem:NoAuthorProvided"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitle() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, false, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[title]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("title", "An awesome paper on JabRef");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("AnawesomepaperonJabRef"), entry.getCiteKeyOptional());
    }

    @Test
    public void generateKeyTitleAbbr() {
        preferences = new BibtexKeyPatternPreferences("", "", false, true, false, pattern);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(pattern);
        bibtexKeyPattern.setDefaultValue("[title:abbr]");
        metadata.setBibtexKeyPattern(bibtexKeyPattern);
        BibEntry entry = new BibEntry();
        entry.setField("title", "An awesome paper on JabRef");
        BibtexKeyPatternUtil.makeLabel(metadata, database, entry, preferences);
        assertEquals(Optional.of("AapoJ"), entry.getCiteKeyOptional());
    }
}
