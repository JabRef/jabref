package org.jabref.logic.bibtex.comparator;

import java.util.Collections;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibStringDiffTest {

    private final BibDatabase originalDataBase = mock(BibDatabase.class);
    private final BibDatabase newDataBase = mock(BibDatabase.class);
    private final BibStringDiff diff = new BibStringDiff(new BibtexString("name2", "content2"), new BibtexString("name2", "content3"));

    @BeforeEach
    void setUp() {
        when(originalDataBase.hasNoStrings()).thenReturn(false);
        when(newDataBase.hasNoStrings()).thenReturn(false);
    }

    @Test
    void compareTest() {
        when(originalDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content"), new BibtexString("name2", "content2")));
        when(newDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content"), new BibtexString("name2", "content3")));

        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        assertEquals(List.of(diff), result);
    }

    @Test
    void equalTest() {
        BibStringDiff other = new BibStringDiff(diff.getOriginalString(), diff.getNewString());
        assertEquals(diff, other);
        assertEquals(diff.hashCode(), other.hashCode());
    }

    @Test
    void notEqualTest() {
        BibStringDiff other = new BibStringDiff(diff.getNewString(), diff.getOriginalString());
        assertNotEquals(diff, other);
        assertNotEquals(diff.hashCode(), other.hashCode());
    }

    @Test
    void identicalObjectsAreEqual() {
        BibStringDiff other = diff;
        assertEquals(other, diff);
    }

    @Test
    void compareToNullObjectIsFalse() {
        assertNotEquals(null, diff);
    }

    @Test
    void compareToDifferentClassIsFalse() {
        assertNotEquals(diff, new Object());
    }

    @Test
    void testGetters() {
        BibtexString bsOne = new BibtexString("aKahle", "Kahle, Brewster");
        BibtexString bsTwo = new BibtexString("iMIT", "Institute of Technology");
        BibStringDiff diff = new BibStringDiff(bsOne, bsTwo);
        assertEquals(diff.getOriginalString(), bsOne);
        assertEquals(diff.getNewString(), bsTwo);
    }

    @Test
    void testCompareEmptyDatabases() {
        when(originalDataBase.hasNoStrings()).thenReturn(true);
        when(newDataBase.hasNoStrings()).thenReturn(true);

        assertEquals(Collections.emptyList(), BibStringDiff.compare(originalDataBase, newDataBase));
    }

    @Test
    void testCompareNameChange() {
        when(originalDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content")));
        when(newDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name2", "content")));

        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        BibStringDiff expectedDiff = new BibStringDiff(new BibtexString("name", "content"), new BibtexString("name2", "content"));
        assertEquals(List.of(expectedDiff), result);
    }

    @Test
    void testCompareNoDiff() {
        when(originalDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content")));
        when(newDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content")));

        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testCompareRemovedString() {
        when(originalDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content")));
        when(newDataBase.getStringValues()).thenReturn(Collections.emptyList());

        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        BibStringDiff expectedDiff = new BibStringDiff(new BibtexString("name", "content"), null);
        assertEquals(List.of(expectedDiff), result);
    }

    @Test
    void testCompareAddString() {
        when(originalDataBase.getStringValues()).thenReturn(Collections.emptyList());
        when(newDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content")));

        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        BibStringDiff expectedDiff = new BibStringDiff(null, new BibtexString("name", "content"));
        assertEquals(List.of(expectedDiff), result);
    }
}
