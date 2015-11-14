package net.sf.jabref.logic.journals;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class AbbreviationsTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testInitializeJournalNames() {
        Abbreviations.initializeJournalNames(Globals.prefs);
    }

    @Test
    public void testIEEEabrv() {
        Boolean oldIEEEsetting = Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV);
        Globals.prefs.putBoolean(JabRefPreferences.USE_IEEE_ABRV, true);
        Abbreviations.initializeJournalNames(Globals.prefs);
        String textAbrv = "#IEEE_J_PROC#";
        String textFull = "Proceedings of the IEEE";
        assertEquals(textAbrv, Abbreviations.journalAbbrev.getNextAbbreviation(textFull).orElse(textFull));
        assertEquals(textFull, Abbreviations.journalAbbrev.getNextAbbreviation(textAbrv).orElse(textAbrv));

        Globals.prefs.putBoolean(JabRefPreferences.USE_IEEE_ABRV, false);
        Abbreviations.initializeJournalNames(Globals.prefs);
        textAbrv = "Proc. IEEE";
        String textAbrv2 = "Proc IEEE";
        assertEquals(textAbrv, Abbreviations.journalAbbrev.getNextAbbreviation(textFull).orElse(textFull));
        assertEquals(textAbrv2, Abbreviations.journalAbbrev.getNextAbbreviation(textAbrv).orElse(textAbrv));
        assertEquals(textFull, Abbreviations.journalAbbrev.getNextAbbreviation(textAbrv2).orElse(textAbrv2));

        Globals.prefs.putBoolean(JabRefPreferences.USE_IEEE_ABRV, oldIEEEsetting);
    }

}
