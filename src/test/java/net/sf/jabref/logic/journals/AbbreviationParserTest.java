package net.sf.jabref.logic.journals;

import static org.junit.Assert.*;
import org.junit.Test;

public class AbbreviationParserTest {

    @Test
    public void testReadJournalListFromResource() {
        AbbreviationParser ap = new AbbreviationParser();
        ap.readJournalListFromResource("/journals/journalList.txt");
         assertFalse(ap.getAbbreviations().isEmpty());
    }
}