package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.event.TestEventListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibDatabaseTest {

    private BibDatabase database;

    @BeforeEach
    public void setUp() {
        database = new BibDatabase();
    }

    @Test
    public void insertEntryAddsEntryToEntriesList() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);
        assertEquals(1, database.getEntries().size());
        assertEquals(1, database.getEntryCount());
        assertEquals(entry, database.getEntries().get(0));
    }

    @Test
    public void containsEntryIdFindsEntry() {
        BibEntry entry = new BibEntry();
        assertFalse(database.containsEntryWithId(entry.getId()));
        database.insertEntry(entry);
        assertTrue(database.containsEntryWithId(entry.getId()));
    }

    @Test
    public void insertEntryWithSameIdThrowsException() {
        BibEntry entry0 = new BibEntry();
        database.insertEntry(entry0);

        BibEntry entry1 = new BibEntry();
        entry1.setId(entry0.getId());
        assertThrows(KeyCollisionException.class, () -> database.insertEntry(entry1));
    }

    @Test
    public void removeEntryRemovesEntryFromEntriesList() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        database.removeEntry(entry);
        assertEquals(Collections.emptyList(), database.getEntries());
        assertFalse(database.containsEntryWithId(entry.getId()));
    }

    @Test
    public void insertNullEntryThrowsException() {
        assertThrows(NullPointerException.class, () -> database.insertEntry(null));
    }

    @Test
    public void removeNullEntryThrowsException() {
        assertThrows(NullPointerException.class, () -> database.removeEntry(null));
    }

    @Test
    public void emptyDatabaseHasNoStrings() {
        assertEquals(Collections.emptySet(), database.getStringKeySet());
        assertTrue(database.hasNoStrings());
    }

    @Test
    public void insertStringUpdatesStringList() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        database.addString(string);
        assertFalse(database.hasNoStrings());
        assertEquals(1, database.getStringKeySet().size());
        assertEquals(1, database.getStringCount());
        assertTrue(database.getStringValues().contains(string));
        assertTrue(database.getStringKeySet().contains(string.getId()));
        assertEquals(string, database.getString(string.getId()));
    }

    @Test
    public void removeStringUpdatesStringList() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        database.addString(string);
        database.removeString(string.getId());
        assertTrue(database.hasNoStrings());
        assertEquals(0, database.getStringKeySet().size());
        assertEquals(0, database.getStringCount());
        assertFalse(database.getStringValues().contains(string));
        assertFalse(database.getStringKeySet().contains(string.getId()));
        assertNull(database.getString(string.getId()));
    }

    @Test
    public void hasStringLabelFindsString() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        database.addString(string);
        assertTrue(database.hasStringLabel("DSP"));
        assertFalse(database.hasStringLabel("VLSI"));
    }

    @Test
    public void setSingleStringAsCollection() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        List<BibtexString> strings = Arrays.asList(string);
        database.setStrings(strings);
        assertEquals(Optional.of(string), database.getStringByName("DSP"));
    }

    @Test
    public void setStringAsCollectionWithUpdatedContentOverridesString() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        BibtexString newContent = new BibtexString("DSP", "ABCD");
        List<BibtexString> strings = Arrays.asList(string, newContent);
        database.setStrings(strings);
        assertEquals(Optional.of(newContent), database.getStringByName("DSP"));
    }

    @Test
    public void setStringAsCollectionWithNewContent() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        BibtexString vlsi = new BibtexString("VLSI", "Very Large Scale Integration");
        List<BibtexString> strings = Arrays.asList(string, vlsi);
        database.setStrings(strings);
        assertEquals(Optional.of(string), database.getStringByName("DSP"));
        assertEquals(Optional.of(vlsi), database.getStringByName("VLSI"));
    }

    @Test
    public void addSameStringLabelTwiceThrowsKeyCollisionException() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        database.addString(string);
        final BibtexString finalString = new BibtexString("DSP", "Digital Signal Processor");

        assertThrows(KeyCollisionException.class, () -> database.addString(finalString));
    }

    @Test
    public void addSameStringIdTwiceThrowsKeyCollisionException() {
        BibtexString string = new BibtexString("DSP", "Digital Signal Processing");
        string.setId("duplicateid");
        database.addString(string);
        final BibtexString finalString = new BibtexString("VLSI", "Very Large Scale Integration");
        finalString.setId("duplicateid");

        assertThrows(KeyCollisionException.class, () -> database.addString(finalString));
    }

    @Test
    public void insertEntryPostsAddedEntryEvent() {
        BibEntry expectedEntry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.registerListener(tel);
        database.insertEntry(expectedEntry);
        assertEquals(expectedEntry, tel.getAddedEntry());
        assertEquals(expectedEntry, tel.getFirstInsertedEntry());
    }

    @Test
    public void insertMultipleEntriesPostsAddedEntryEvent() {
        BibEntry firstEntry = new BibEntry();
        BibEntry secondEntry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.registerListener(tel);
        database.insertEntries(firstEntry, secondEntry);
        assertEquals(firstEntry, tel.getFirstInsertedEntry());
        assertEquals(secondEntry, tel.getAddedEntry());
    }

    @Test
    public void removeEntryPostsRemovedEntryEvent() {
        BibEntry expectedEntry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.insertEntry(expectedEntry);
        database.registerListener(tel);
        database.removeEntry(expectedEntry);
        BibEntry actualEntry = tel.getRemovedEntry();
        assertEquals(expectedEntry, actualEntry);
    }

    @Test
    public void changingEntryPostsChangeEntryEvent() {
        BibEntry entry = new BibEntry();
        TestEventListener tel = new TestEventListener();
        database.insertEntry(entry);
        database.registerListener(tel);

        entry.setField(new UnknownField("test"), "some value");

        assertEquals(entry, tel.getChangedEntry());
    }

    @Test
    public void correctKeyCountOne() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void correctKeyCountTwo() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        assertEquals(2, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void correctKeyCountAfterRemoving() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        entry = new BibEntry();
        entry.setCiteKey("AAA");
        database.insertEntry(entry);
        database.removeEntry(entry);
        assertEquals(1, database.getDuplicationChecker().getNumberOfKeyOccurrences("AAA"));
    }

    @Test
    public void circularStringResolving() {
        BibtexString string = new BibtexString("AAA", "#BBB#");
        database.addString(string);
        string = new BibtexString("BBB", "#AAA#");
        database.addString(string);
        assertEquals("AAA", database.resolveForStrings("#AAA#"));
        assertEquals("BBB", database.resolveForStrings("#BBB#"));
    }

    @Test
    public void circularStringResolvingLongerCycle() {
        BibtexString string = new BibtexString("AAA", "#BBB#");
        database.addString(string);
        string = new BibtexString("BBB", "#CCC#");
        database.addString(string);
        string = new BibtexString("CCC", "#DDD#");
        database.addString(string);
        string = new BibtexString("DDD", "#AAA#");
        database.addString(string);
        assertEquals("AAA", database.resolveForStrings("#AAA#"));
        assertEquals("BBB", database.resolveForStrings("#BBB#"));
        assertEquals("CCC", database.resolveForStrings("#CCC#"));
        assertEquals("DDD", database.resolveForStrings("#DDD#"));
    }

    @Test
    public void resolveForStringsMonth() {
        assertEquals("January", database.resolveForStrings("#jan#"));
    }

    @Test
    public void resolveForStringsSurroundingContent() {
        BibtexString string = new BibtexString("AAA", "aaa");
        database.addString(string);
        assertEquals("aaaaaAAA", database.resolveForStrings("aa#AAA#AAA"));
    }

    @Test
    public void resolveForStringsOddHashMarkAtTheEnd() {
        BibtexString string = new BibtexString("AAA", "aaa");
        database.addString(string);
        assertEquals("AAAaaaAAA#", database.resolveForStrings("AAA#AAA#AAA#"));
    }

    @Test
    public void getUsedStrings() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "#AAA#");
        BibtexString tripleA = new BibtexString("AAA", "Some other #BBB#");
        BibtexString tripleB = new BibtexString("BBB", "Some more text");
        BibtexString tripleC = new BibtexString("CCC", "Even more text");
        Set<BibtexString> stringSet = new HashSet<>();
        stringSet.add(tripleA);
        stringSet.add(tripleB);

        database.addString(tripleA);
        database.addString(tripleB);
        database.addString(tripleC);
        database.insertEntry(entry);

        Set<BibtexString> usedStrings = new HashSet<>(database.getUsedStrings(Arrays.asList(entry)));
        assertEquals(stringSet, usedStrings);
    }

    @Test
    public void getUsedStringsSingleString() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "#AAA#");
        BibtexString tripleA = new BibtexString("AAA", "Some other text");
        BibtexString tripleB = new BibtexString("BBB", "Some more text");
        List<BibtexString> strings = new ArrayList<>(1);
        strings.add(tripleA);

        database.addString(tripleA);
        database.addString(tripleB);
        database.insertEntry(entry);

        List<BibtexString> usedStrings = (List<BibtexString>) database.getUsedStrings(Arrays.asList(entry));
        assertEquals(strings, usedStrings);
    }

    @Test
    public void getUsedStringsNoString() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Oscar Gustafsson");
        BibtexString string = new BibtexString("AAA", "Some other text");
        database.addString(string);
        database.insertEntry(entry);
        Collection<BibtexString> usedStrings = database.getUsedStrings(Arrays.asList(entry));
        assertEquals(Collections.emptyList(), usedStrings);
    }

    @Test
    public void getEntriesSortedWithTwoEntries() {
        BibEntry entryB = new BibEntry(StandardEntryType.Article);
        entryB.setId("2");
        BibEntry entryA = new BibEntry(StandardEntryType.Article);
        entryB.setId("1");
        database.insertEntries(entryB, entryA);
        assertEquals(Arrays.asList(entryA, entryB), database.getEntriesSorted(Comparator.comparing(BibEntry::getId)));
    }

    @Test
    public void preambleIsEmptyIfNotSet() {
        assertEquals(Optional.empty(), database.getPreamble());
    }

    @Test
    public void setPreambleWorks() {
        database.setPreamble("Oh yeah!");
        assertEquals(Optional.of("Oh yeah!"), database.getPreamble());
    }
}
