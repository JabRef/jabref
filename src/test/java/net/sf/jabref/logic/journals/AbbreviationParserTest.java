package net.sf.jabref.logic.journals;

import static org.junit.Assert.*;
import org.junit.Test;

public class AbbreviationParserTest {

    @Test
    public void testReadJournalListFromResource() throws Exception {
        AbbreviationParser ap = new AbbreviationParser();
        ap.readJournalListFromResource(Abbreviations.JOURNALS_FILE_BUILTIN);
         assertFalse(ap.getAbbreviations().isEmpty());
    }
}