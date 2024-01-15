package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserResultTest {

    @Test
    void isEmptyForNewParseResult() {
        ParserResult empty = new ParserResult();
        assertTrue(empty.isEmpty());
    }

    @Test
    void isNotEmptyForBibDatabaseWithOneEntry() {
        BibEntry bibEntry = new BibEntry();
        BibDatabase bibDatabase = new BibDatabase(List.of(bibEntry));
        ParserResult parserResult = new ParserResult(bibDatabase);
        assertFalse(parserResult.isEmpty());
    }

    @Test
    public void warningsAddedMatchErrorMessage() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Warning 1 ");
        parserResult.addWarning("Warning 2 ");
        assertEquals("Warning 1 \nWarning 2 ", parserResult.getErrorMessage());
    }

    @Test
    public void hasEmptyMessageForNoWarnings() {
        ParserResult parserResult = new ParserResult();
        assertEquals("", parserResult.getErrorMessage());
    }

    @Test
    public void doesNotHaveDuplicateWarnings() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Duplicate Warning");
        parserResult.addWarning("Duplicate Warning");
        assertEquals("Duplicate Warning", parserResult.getErrorMessage());
    }
}
