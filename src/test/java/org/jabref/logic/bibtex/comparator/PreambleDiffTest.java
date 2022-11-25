package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
