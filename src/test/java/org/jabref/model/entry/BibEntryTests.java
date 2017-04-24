package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BibEntryTests {

    private BibEntry keywordEntry;
    private BibEntry emptyEntry;



    @Before
    public void setUp() {
        // Default entry for most keyword and some type tests
        keywordEntry = new BibEntry();
        keywordEntry.setType(BibtexEntryTypes.ARTICLE);
        keywordEntry.setField("keywords", "Foo, Bar");
        keywordEntry.setChanged(false);

        // Empty entry for some tests
        emptyEntry = new BibEntry();
        emptyEntry.setType("article");
        emptyEntry.setChanged(false);

    }

    @Test
    public void testDefaultConstructor() {
        BibEntry entry = new BibEntry();
        // we have to use `getType("misc")` in the case of biblatex mode
        assertEquals("misc", entry.getType());
        Assert.assertNotNull(entry.getId());
        Assert.assertFalse(entry.getField("author").isPresent());
    }

    @Test
    public void allFieldsPresentDefault() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        List<String> requiredFields = new ArrayList<>();

        requiredFields.add("author");
        requiredFields.add("title");
        Assert.assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add("year");
        Assert.assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void allFieldsPresentOr() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        List<String> requiredFields = new ArrayList<>();

        // XOR required
        requiredFields.add("journal/year");
        Assert.assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add("year/address");
        Assert.assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test(expected = NullPointerException.class)
    public void isNullCiteKeyThrowsNPE() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());

        e.setCiteKey(null);
        Assert.fail();
    }

    @Test
    public void isEmptyCiteKey() {
        BibEntry e = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        Assert.assertFalse(e.hasCiteKey());

        e.setCiteKey("");
        Assert.assertFalse(e.hasCiteKey());

        e.setCiteKey("key");
        Assert.assertTrue(e.hasCiteKey());

        e.clearField(BibEntry.KEY_FIELD);
        Assert.assertFalse(e.hasCiteKey());
    }

    @Test
    public void typeOfBibEntryIsMiscAfterSettingToNullString() {
        assertEquals("article", keywordEntry.getType());
        keywordEntry.setType((String) null);
        assertEquals("misc", keywordEntry.getType());
    }

    @Test
    public void typeOfBibEntryIsMiscAfterSettingToEmptyString() {
        assertEquals("article", keywordEntry.getType());
        keywordEntry.setType("");
        assertEquals("misc", keywordEntry.getType());
    }


    @Test
    public void getFieldOrAliasDateWithYearNumericalMonthString() {
        emptyEntry.setField("year", "2003");
        emptyEntry.setField("month", "3");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias("date"));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonth() {
        emptyEntry.setField("year", "2003");
        emptyEntry.setField("month", "#mar#");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias("date"));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonthString() {
        emptyEntry.setField("year", "2003");
        emptyEntry.setField("month", "mar");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias("date"));
    }

    @Test
    public void getFieldOrAliasDateWithOnlyYear() {
        emptyEntry.setField("year", "2003");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias("date"));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYY() {
        emptyEntry.setField("date", "2003");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias("year"));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMM() {
        emptyEntry.setField("date", "2003-03");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias("year"));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMMDD() {
        emptyEntry.setField("date", "2003-03-30");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias("year"));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYReturnsNull() {
        emptyEntry.setField("date", "2003");
        assertEquals(Optional.empty(), emptyEntry.getFieldOrAlias("month"));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMM() {
        emptyEntry.setField("date", "2003-03");
        assertEquals(Optional.of("#mar#"), emptyEntry.getFieldOrAlias("month"));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMMDD() {
        emptyEntry.setField("date", "2003-03-30");
        assertEquals(Optional.of("#mar#"), emptyEntry.getFieldOrAlias("month"));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeValueIsUnchanged() {
        emptyEntry.setField("title", "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), emptyEntry.getFieldOrAliasLatexFree("title"));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeAliasValueIsUnchanged() {
        emptyEntry.setField("journal", "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), emptyEntry.getFieldOrAliasLatexFree("journaltitle"));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemoved() {
        emptyEntry.setField("title", "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), emptyEntry.getFieldOrAliasLatexFree("title"));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemovedFromAlias() {
        emptyEntry.setField("journal", "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), emptyEntry.getFieldOrAliasLatexFree("journaltitle"));
    }

    @Test
    public void getFieldOrAliasLatexFreeComplexConversionInAlias() {
        emptyEntry.setField("journal", "A 32~{mA} {$\\Sigma\\Delta$}-modulator");
        assertEquals(Optional.of("A 32 mA ΣΔ-modulator"), emptyEntry.getFieldOrAliasLatexFree("journaltitle"));
    }

    @Test(expected = NullPointerException.class)
    public void setNullField() {
        emptyEntry.setField(null);
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void addNullKeywordThrowsNPE() {
        keywordEntry.addKeyword((Keyword)null, ',');
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void putNullKeywordListThrowsNPE() {
        keywordEntry.putKeywords((KeywordList)null, ',');
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void putNullKeywordSeparatorThrowsNPE() {
        keywordEntry.putKeywords(Arrays.asList("A", "B"), null);
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
        Assert.assertTrue(keywordEntry.hasChanged());
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
        Assert.assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testAddKeywordEmptyKeywordIsNotAdded() {
        keywordEntry.addKeyword("", ',');
        assertEquals(new KeywordList("Foo", "Bar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordEmptyKeywordNotChanged() {
        keywordEntry.addKeyword("", ',');
        Assert.assertFalse(keywordEntry.hasChanged());
    }

    @Test
    public void texNewBibEntryHasNoKeywords() {
        Assert.assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryHasNoKeywordsEvenAfterAddingEmptyKeyword() {
        emptyEntry.addKeyword("", ',');
        Assert.assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryAfterAddingEmptyKeywordNotChanged() {
        emptyEntry.addKeyword("", ',');
        Assert.assertFalse(emptyEntry.hasChanged());
    }

    @Test
    public void testAddKeywordsWorksAsExpected() {
        emptyEntry.addKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(new KeywordList("Foo", "Bar"), emptyEntry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsOverwritesOldKeywords() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertEquals(new KeywordList("Yin", "Yang"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsHasChanged() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        Assert.assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListErasesPreviousKeywords() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        Assert.assertTrue(keywordEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListHasChanged() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        Assert.assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentry() {
        emptyEntry.putKeywords(Collections.emptyList(), ',');
        Assert.assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentryNotChanged() {
        emptyEntry.putKeywords(Collections.emptyList(), ',');
        Assert.assertFalse(emptyEntry.hasChanged());
    }

    @Test
    public void putKeywordsToEmptyReturnsNoChange() {
        Optional<FieldChange> change = emptyEntry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void clearKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, "keywords", "Foo, Bar", null)), change);
    }

    @Test
    public void changeKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Arrays.asList("Test", "FooTest"), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, "keywords", "Foo, Bar", "Test, FooTest")),
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
        entry.setField(FieldName.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(new KeywordList("w1", "w2a w2b", "w3"), entry.getKeywords(','));
    }

    @Test
    public void testGroupAndSearchHits() {
        BibEntry be = new BibEntry();
        be.setGroupHit(true);
        Assert.assertTrue(be.isGroupHit());
        be.setGroupHit(false);
        Assert.assertFalse(be.isGroupHit());
        be.setSearchHit(true);
        Assert.assertTrue(be.isSearchHit());
        be.setSearchHit(false);
        Assert.assertFalse(be.isSearchHit());

    }

    @Test
    public void setCiteKey() {
        BibEntry be = new BibEntry();
        Assert.assertFalse(be.hasCiteKey());
        be.setField("author", "Albert Einstein");
        be.setCiteKey("Einstein1931");
        Assert.assertTrue(be.hasCiteKey());
        assertEquals(Optional.of("Einstein1931"), be.getCiteKeyOptional());
        assertEquals(Optional.of("Albert Einstein"), be.getField("author"));
        be.clearField("author");
        assertEquals(Optional.empty(), be.getField("author"));
    }
}
