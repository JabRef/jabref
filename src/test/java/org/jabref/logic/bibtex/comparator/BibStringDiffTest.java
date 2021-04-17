package org.jabref.logic.bibtex.comparator;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibStringDiffTest {

    private final BibDatabase originalDataBase = mock(BibDatabase.class);
    private final BibDatabase newDataBase = mock(BibDatabase.class);
    private final BibtexString str1 = new BibtexString("name", "content");
    private final BibtexString str2 = new BibtexString("name2", "content2");
    private final BibtexString str3 = new BibtexString("name2", "content3");
    private final BibStringDiff diff = new BibStringDiff(str2, str3);

    @BeforeEach
    void setUp() {
        when(originalDataBase.hasNoStrings()).thenReturn(false);
        when(newDataBase.hasNoStrings()).thenReturn(false);
        when(originalDataBase.getStringValues()).thenReturn(Arrays.asList(str1, str2));
        when(newDataBase.getStringValues()).thenReturn(Arrays.asList(str1, str3));
    }

    @Test
    void compareTest() {
        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        assertEquals(diff.getOriginalString().toString(), result.get(0).getOriginalString().toString());
        assertEquals(diff.getNewString().toString(), result.get(0).getNewString().toString());
    }
}
