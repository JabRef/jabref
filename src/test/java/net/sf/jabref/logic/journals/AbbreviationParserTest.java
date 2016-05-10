package net.sf.jabref.logic.journals;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class AbbreviationParserTest {

    @Test
    public void testReadJournalListFromResource() {
        AbbreviationParser ap = new AbbreviationParser();
        ap.readJournalListFromResource("/journals/journalList.txt");
         assertFalse(ap.getAbbreviations().isEmpty());
    }
}