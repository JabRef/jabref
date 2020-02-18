package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryComparatorTest {
    @Test
    void recognizeIdenticObjectsAsEqual() {
        BibEntry e1 = new BibEntry();
        BibEntry e2 = e1;
        assertEquals(0, new EntryComparator(false, false, StandardField.TITLE).compare(e1, e2));
    }

    /**
     * Test if the compare function correctly sorts two BibTex entries with
     * author names only having one differing letter.
     */
    @Test
    void testEntryComparator1() {
        BibEntry e1 = new BibEntry(StandardEntryType.Article);        
        e1.setField(StandardField.AUTHOR, "ABB");        
        
        BibEntry e2 = new BibEntry(StandardEntryType.Article);        
        e2.setField(StandardField.AUTHOR, "ABC");        

        assertEquals(-1, new EntryComparator(false, false, StandardField.AUTHOR).compare(e1, e2));
        assertEquals(1, new EntryComparator(false, false, StandardField.AUTHOR).compare(e2, e1));
    }
    
    /**
     * Test if compare function correctly sorts two BibTex entries in
     * regards to the surname of the author
     */
    @Test
    void testEntryComparator2() {
        BibEntry e1 = new BibEntry(StandardEntryType.Article);        
        e1.setField(StandardField.AUTHOR, "ABC ABB");        
        
        BibEntry e2 = new BibEntry(StandardEntryType.Article);        
        e2.setField(StandardField.AUTHOR, "ABB ABC");        

        assertEquals(1, new EntryComparator(false, false, StandardField.AUTHOR).compare(e1, e2));
    }

    /**
     * Test if the compare function manages to compare two BibTex entries in
     * regards to the correct specified field.
     */
    @Test
    void testEntryComparator3() {
        BibEntry e1 = new BibEntry(StandardEntryType.Article);        
        e1.setField(StandardField.AUTHOR, "ABC");    
        e1.setField(StandardField.TITLE, "ABB");        
        
        BibEntry e2 = new BibEntry(StandardEntryType.Article);        
        e2.setField(StandardField.AUTHOR, "ABB");        
        e1.setField(StandardField.TITLE, "ABC");        

        assertEquals(-1, new EntryComparator(false, false, StandardField.TITLE).compare(e1, e2));
    }

    /**
     * Test if compare function manages to compare two different BibTex entries in
     * regards to a numeric field.
     */
    void testEntryComparator4() {
        BibEntry e1 = new BibEntry(StandardEntryType.Article);                
        e1.setField(StandardField.AUTHOR, "Niklas Andersson");        
        e1.setField(StandardField.TITLE, "Reinforcement Learning - A Case Study");        
        e1.setField(StandardField.YEAR, "2010");        
        
        BibEntry e2 = new BibEntry(StandardEntryType.Article);        
        e2.setField(StandardField.AUTHOR, "Niklas Svensson");       
        e1.setField(StandardField.TITLE, "Reinforcement Learning - A Litterature Study");        
        e1.setField(StandardField.YEAR, "2015");        

        assertEquals(-1, new EntryComparator(false, false, StandardField.YEAR).compare(e1, e2));
    }
}
