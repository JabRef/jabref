package org.jabref.logic.bibtex.comparator;

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
    private final BibStringDiff diff = new BibStringDiff(new BibtexString("name2", "content2"), new BibtexString("name2", "content3"));

    @BeforeEach
    void setUp() {
        when(originalDataBase.hasNoStrings()).thenReturn(false);
        when(newDataBase.hasNoStrings()).thenReturn(false);
        when(originalDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content"), new BibtexString("name2", "content2")));
        when(newDataBase.getStringValues()).thenReturn(List.of(new BibtexString("name", "content"), new BibtexString("name2", "content3")));
    }

    @Test
    void compareTest() {
        List<BibStringDiff> result = BibStringDiff.compare(originalDataBase, newDataBase);
        assertEquals(List.of(diff), result);
    }

    @Test
    void compareTestDefault() throws Exception {

        BibtexString BibStringDiff_that  = new BibtexString("name1", "content1");
        BibtexString BibStringDiff_other  = new BibtexString("name2", "content2");

        Boolean result = BibStringDiff_that .equals(BibStringDiff_other);

        assertEquals(true, result);
    }
    @Test
    void compareTestNull() throws Exception {

        BibtexString BibStringDiff_that  = new BibtexString("name1", "content1");
        BibtexString BibStringDiff_other = null;

        Boolean result = BibStringDiff_that.equals(BibStringDiff_other);

        assertEquals(false, result);
    }

    @Test
    void compareDiferentTestDefault() throws Exception {

        BibtexString BibStringDiff_that  = new BibtexString("name1", "content1");
        String BibStringDiff_other = "bora";

        Boolean result = BibStringDiff_that.equals(BibStringDiff_other);

        assertEquals(false, result);
    }

    @Test
    void compareSameTestDefault() throws Exception {

      
        BibtexString BibStringDiff_that  = new BibtexString("name1", "content1");
        BibtexString BibStringDiff_other  = new BibtexString("name2", "content2");

        Boolean result = BibStringDiff_that.equals(BibStringDiff_other);

        assertEquals(false, result);
    }
}
