package net.sf.jabref.logic.journals;

import net.sf.jabref.Globals;

import static org.junit.Assert.*;
import org.junit.Test;

public class AbbreviationParserTest {

    @Test
    public void testReadJournalListFromResource() throws Exception {
        AbbreviationParser ap = new AbbreviationParser();
        ap.readJournalListFromResource(Globals.JOURNALS_FILE_BUILTIN);
         assertFalse(ap.getAbbreviations().isEmpty());
    }
}