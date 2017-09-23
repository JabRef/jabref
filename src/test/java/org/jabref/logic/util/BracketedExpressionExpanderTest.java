package org.jabref.logic.util;

import org.jabref.model.entry.BibEntry;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BracketedExpressionExpanderTest {

    private BibEntry bibentry;

    @Before
    public void setUp() throws Exception {
        bibentry = new BibEntry();
        bibentry.setField("author", "O. Kitsune");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");
    }

    @Test
    public void bibentryExpansionTest() {
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals(bex.expandBrackets("[year]_[auth]_[firstpage]"), "2017_Kitsune_213");
    }
}
