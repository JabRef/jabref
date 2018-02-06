package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class AbbreviationParserTest {

    @Test
    public void testReadJournalListFromResource() {
        AbbreviationParser ap = new AbbreviationParser();
        ap.readJournalListFromResource("/journals/journalList.txt");
         assertFalse(ap.getAbbreviations().isEmpty());
    }
}
