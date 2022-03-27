package org.jabref.logic.bibtex.comparator;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreambleDiffTest {

    private final BibDatabaseContext originalDataBaseContext = mock(BibDatabaseContext.class);
    private final BibDatabaseContext newDataBaseContext = mock(BibDatabaseContext.class);
    private final BibDatabase originalDataBase = mock(BibDatabase.class);
    private final BibDatabase newDataBase = mock(BibDatabase.class);

    @BeforeEach
    void setUp() {
        when(originalDataBaseContext.getDatabase()).thenReturn(originalDataBase);
        when(newDataBaseContext.getDatabase()).thenReturn(newDataBase);
    }

    @Test
    void compareSamePreambleTest() {
        when(originalDataBase.getPreamble()).thenReturn(Optional.of("preamble"));
        when(newDataBase.getPreamble()).thenReturn(Optional.of("preamble"));

        assertEquals(Optional.empty(), PreambleDiff.compare(originalDataBaseContext, newDataBaseContext));
    }

    @Test
    void compareDifferentPreambleTest() {
        when(originalDataBase.getPreamble()).thenReturn(Optional.of("preamble"));
        when(newDataBase.getPreamble()).thenReturn(Optional.of("otherPreamble"));

        Optional<PreambleDiff> expected = Optional.of(new PreambleDiff("preamble", "otherPreamble"));
        Optional<PreambleDiff> result = PreambleDiff.compare(originalDataBaseContext, newDataBaseContext);
        assertEquals(expected, result);
    }

    @Test // Case test 3
    void equalNullPreambleTest() {
        PreambleDiff preambletest = new PreambleDiff("original", "new");
        assertTrue(!preambletest.equals(null));
    }

    @Test // Case test 1
    void equalOtherPreambleTest() {
        PreambleDiff preambletest = new PreambleDiff("original", "new");
        assertTrue(preambletest.equals(preambletest));
    }

    @Test // Case test 4
    void equalSameClassTest() {
        PreambleDiff preambletest = new PreambleDiff("original", "new");
        String otherclasstest = new String("string");
        assertTrue(!preambletest.equals(otherclasstest));
    }

    @Test // Case test 2
    void equalSameAttributesTest() {
        PreambleDiff preambletest = new PreambleDiff("original", "new");
        PreambleDiff preambleattributestest = new PreambleDiff("original", "new");
        assertTrue(preambletest.equals(preambleattributestest));
    }

    @Test // Case test 5
    void equalDiferentAttributesTest() {
        PreambleDiff preambletest = new PreambleDiff("original", "new");
        PreambleDiff preambleattributestest = new PreambleDiff("original false", "new false");
        assertTrue(!preambletest.equals(preambleattributestest));
    }
}
