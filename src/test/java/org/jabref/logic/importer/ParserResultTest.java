package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

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
}
