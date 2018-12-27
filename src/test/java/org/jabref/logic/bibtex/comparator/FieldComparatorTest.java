package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldComparatorTest {
    @Test
    public void compareMonthFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator("month");
        BibEntry equal = new BibEntry();
        equal.setField("month", "1");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareMonthFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator("month");
        BibEntry equal = new BibEntry();
        equal.setField("month", "1");
        BibEntry equal2 = new BibEntry();
        equal2.setField("month", "1");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareMonthFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator("month");
        BibEntry smaller = new BibEntry();
        smaller.setField("month", "jan");
        BibEntry bigger = new BibEntry();
        bigger.setField("month", "feb");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareMonthFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator("month", true);
        BibEntry smaller = new BibEntry();
        smaller.setField("month", "feb");
        BibEntry bigger = new BibEntry();
        bigger.setField("month", "jan");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator("year");
        BibEntry equal = new BibEntry();
        equal.setField("year", "2016");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareYearFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator("year");
        BibEntry equal = new BibEntry();
        equal.setField("year", "2016");
        BibEntry equal2 = new BibEntry();
        equal2.setField("year", "2016");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareYearFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator("year");
        BibEntry smaller = new BibEntry();
        smaller.setField("year", "2000");
        BibEntry bigger = new BibEntry();
        bigger.setField("year", "2016");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator("year", true);
        BibEntry smaller = new BibEntry();
        smaller.setField("year", "2016");
        BibEntry bigger = new BibEntry();
        bigger.setField("year", "2000");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareTypeFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator("entrytype");
        BibEntry equal = new BibEntry("article");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareTypeFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator("entrytype");
        BibEntry equal = new BibEntry("article");
        equal.setId("1");
        BibEntry equal2 = new BibEntry("article");
        equal2.setId("1");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareTypeFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator("entrytype");
        BibEntry smaller = new BibEntry("article");
        BibEntry bigger = new BibEntry("book");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareTypeFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator("entrytype", true);
        BibEntry bigger = new BibEntry("article");
        BibEntry smaller = new BibEntry("book");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareStringFieldsIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator("title");
        BibEntry equal = new BibEntry();
        equal.setField("title", "title");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareStringFieldsEquality() throws Exception {
        FieldComparator comparator = new FieldComparator("title");
        BibEntry equal = new BibEntry();
        equal.setField("title", "title");
        BibEntry equal2 = new BibEntry();
        equal2.setField("title", "title");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareStringFieldsBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator("title");
        BibEntry bigger = new BibEntry();
        bigger.setField("title", "b");
        BibEntry smaller = new BibEntry();
        smaller.setField("title", "a");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareStringFieldsBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator("title", true);
        BibEntry bigger = new BibEntry();
        bigger.setField("title", "a");
        BibEntry smaller = new BibEntry();
        smaller.setField("title", "b");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void nameOfComparisonField() throws Exception {
        FieldComparator comparator = new FieldComparator("title");
        assertEquals("title", comparator.getFieldName());
    }

    @Test
    public void nameOfComparisonFieldAlias() throws Exception {
        FieldComparator comparator = new FieldComparator("author/editor");
        assertEquals("author/editor", comparator.getFieldName());
    }
}
