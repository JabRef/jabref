package org.jabref.model.database.event;

import org.jabref.model.database.EntrySorter;
import org.jabref.model.entry.BibEntry;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AutosaveEventTest {

    @Test
    public void givenNothingWhenCreatingThenNotNull() {
        AutosaveEvent e = new AutosaveEvent();
        assertNotNull(e);
    }
}
