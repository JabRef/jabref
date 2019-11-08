package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibEntryWithKeywordsTest {

    private BibEntry keywordEntry;

    @BeforeEach
    public void setUp() {
        // Default entry for most keyword and some type tests
        keywordEntry = new BibEntry();
        keywordEntry.setType(StandardEntryType.Article);
        keywordEntry.setField(StandardField.KEYWORDS, "Foo, Bar");
        keywordEntry.setChanged(false);
    }

    @Test
    public void addNullKeywordThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.addKeyword((Keyword) null, ','));
    }

    @Test
    public void putNullKeywordListThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.putKeywords((KeywordList) null, ','));
    }

    @Test
    public void putNullKeywordSeparatorThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.putKeywords(Arrays.asList("A", "B"), null));
    }

    @Test
    public void testGetSeparatedKeywordsAreCorrect() {
        assertEquals(new KeywordList("Foo", "Bar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordIsCorrect() {
        keywordEntry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordHasChanged() {
        keywordEntry.addKeyword("FooBar", ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testAddKeywordTwiceYiedsOnlyOne() {
        keywordEntry.addKeyword("FooBar", ',');
        keywordEntry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void addKeywordIsCaseSensitive() {
        keywordEntry.addKeyword("FOO", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FOO"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordWithDifferentCapitalizationChanges() {
        keywordEntry.addKeyword("FOO", ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testAddKeywordEmptyKeywordIsNotAdded() {
        keywordEntry.addKeyword("", ',');
        assertEquals(new KeywordList("Foo", "Bar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordEmptyKeywordNotChanged() {
        keywordEntry.addKeyword("", ',');
        assertFalse(keywordEntry.hasChanged());
    }

    @Test
    public void texNewBibEntryHasNoKeywords() {
        BibEntry entry = new BibEntry();
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryHasNoKeywordsEvenAfterAddingEmptyKeyword() {
        BibEntry entry = new BibEntry();
        entry.addKeyword("", ',');
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryAfterAddingEmptyKeywordNotChanged() {
        BibEntry entry = new BibEntry();
        entry.addKeyword("", ',');
        assertFalse(entry.hasChanged());
    }

    @Test
    public void testAddKeywordsWorksAsExpected() {
        BibEntry entry = new BibEntry();
        entry.addKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(new KeywordList("Foo", "Bar"), entry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsOverwritesOldKeywords() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertEquals(new KeywordList("Yin", "Yang"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsHasChanged() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListErasesPreviousKeywords() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertTrue(keywordEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListHasChanged() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentry() {
        BibEntry entry = new BibEntry();
        entry.putKeywords(Collections.emptyList(), ',');
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentryNotChanged() {
        BibEntry entry = new BibEntry();
        entry.putKeywords(Collections.emptyList(), ',');
        assertFalse(entry.hasChanged());
    }

    @Test
    public void putKeywordsToEmptyReturnsNoChange() {
        BibEntry entry = new BibEntry();
        Optional<FieldChange> change = entry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void clearKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, StandardField.KEYWORDS, "Foo, Bar", null)), change);
    }

    @Test
    public void changeKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Arrays.asList("Test", "FooTest"), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, StandardField.KEYWORDS, "Foo, Bar", "Test, FooTest")),
                change);
    }

    @Test
    public void putKeywordsToSameReturnsNoChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void getKeywordsReturnsParsedKeywordListFromKeywordsField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(new KeywordList("w1", "w2a w2b", "w3"), entry.getKeywords(','));
    }

    @Test
    public void removeKeywordsOnEntryWithoutKeywordsDoesNothing() {
        BibEntry entry = new BibEntry();
        Optional<FieldChange> change = entry.removeKeywords(SpecialField.RANKING.getKeyWords(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void removeKeywordsWithEmptyListDoesNothing() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(new KeywordList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void removeKeywordsWithNonExistingKeywordsDoesNothing() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(KeywordList.parse("kw3, kw4", ','), ',');
        assertEquals(Optional.empty(), change);
        assertEquals(Sets.newHashSet("kw1", "kw2"), keywordEntry.getKeywords(',').toStringList());
    }

    @Test
    public void removeKeywordsWithExistingKeywordsRemovesThem() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2", "kw3"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(KeywordList.parse("kw1, kw2", ','), ',');
        assertTrue(change.isPresent());
        assertEquals(KeywordList.parse("kw3", ','), keywordEntry.getKeywords(','));
    }

    @Test
    public void keywordListCorrectlyConstructedForThreeKeywords() {
        BibEntry entry = new BibEntry();
        entry.addKeyword("kw", ',');
        entry.addKeyword("kw2", ',');
        entry.addKeyword("kw3", ',');
        KeywordList actual = entry.getKeywords(',');
        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }

    @Test
    public void testGetEmptyResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.CROSSREF, "entry2");
        database.insertEntry(entry);

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("entry2");
        database.insertEntry(entry2);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(), actual);
    }

    @Test
    public void testGetSingleResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.CROSSREF, "entry2");

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("entry2");
        entry2.addKeyword("kw", ',');

        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    public void testGetResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.CROSSREF, "entry2");

        BibEntry entry2 = new BibEntry();
        entry2.setCiteKey("entry2");
        entry2.addKeyword("kw", ',');
        entry2.addKeyword("kw2", ',');
        entry2.addKeyword("kw3", ',');

        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }
}
