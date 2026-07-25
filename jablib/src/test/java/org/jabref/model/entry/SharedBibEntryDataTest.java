package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SharedBibEntryDataTest {

    @Test
    void equalsReturnsTrueWhenCompareToIsZero() {
        SharedBibEntryData data1 = new SharedBibEntryData();
        SharedBibEntryData data2 = new SharedBibEntryData();

        assertEquals(0, data1.compareTo(data2));
        assertEquals(data1, data2);
    }

    @Test
    void equalsReturnsFalseWhenSharedIDsDiffer() {
        SharedBibEntryData data1 = new SharedBibEntryData();
        SharedBibEntryData data2 = new SharedBibEntryData();
        data2.setSharedID(42);

        assertNotEquals(data1, data2);
    }

    @Test
    void equalsReturnsFalseWhenVersionsDiffer() {
        SharedBibEntryData data1 = new SharedBibEntryData();
        SharedBibEntryData data2 = new SharedBibEntryData();
        data2.setVersion(2);

        assertNotEquals(data1, data2);
    }

    @Test
    void equalObjectsHaveSameHashCode() {
        SharedBibEntryData data1 = new SharedBibEntryData();
        SharedBibEntryData data2 = new SharedBibEntryData();

        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }
}
